package ru.mail.my.towers.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.mail.my.towers.api.model.GsonAuthResponse;
import ru.mail.my.towers.api.model.GsonAttackResponse;
import ru.mail.my.towers.api.model.GsonCreateTowerResponse;
import ru.mail.my.towers.api.model.GsonGameInfoResponse;
import ru.mail.my.towers.api.model.GsonGameStatTopResponse;
import ru.mail.my.towers.api.model.GsonLogoutResponse;
import ru.mail.my.towers.api.model.GsonGetProfileResponse;
import ru.mail.my.towers.api.model.GsonMyTowersResponse;
import ru.mail.my.towers.api.model.GsonNotificationsResponse;
import ru.mail.my.towers.api.model.GsonPutProfileResponse;
import ru.mail.my.towers.api.model.GsonRequestAuthResponse;
import ru.mail.my.towers.api.model.GsonRetreatResponse;
import ru.mail.my.towers.api.model.GsonTowerInfoRespomse;
import ru.mail.my.towers.api.model.GsonTowersInfoResponse;
import ru.mail.my.towers.api.model.GsonUpdateTowerResponse;
import ru.mail.my.towers.diagnostics.Logger;
import ru.mail.my.towers.model.GameStatMode;
import ru.mail.my.towers.model.TowerUpdateAction;

import static ru.mail.my.towers.TowersApp.prefs;

public interface TowersGameApi {
    String BASE_URL = "http://levchurakov.com:5590/api/v1/";

    /**
     * Запрос СМС с кодом
     *
     * @param phone    номер телефона (обязательно, только цифры, от 3 до 30 символов)
     * @param language язык пользователя (обязательно, ISO 639-2).
     */
    @POST("login")
    @FormUrlEncoded
    Call<GsonRequestAuthResponse> requestAuth(@Field("phone") String phone, @Field("language") String language);

    /**
     * Подтверждение кода
     *
     * @param phone номер телефона (обязательно, только цифры, от 3 до 30 символов)
     * @param code  код из СМС (обязательно, только цифры, 4 символа).
     */
    @POST("login")
    @FormUrlEncoded
    Call<GsonAuthResponse> auth(@Field("phone") String phone, @Field("code") String code);

    /**
     * Выход из системы
     */
    @POST("logout")
    @FormUrlEncoded
    Call<GsonLogoutResponse> logout();

    /**
     * Получение своего профиля
     */
    @GET("me/profile")
    Call<GsonGetProfileResponse> getMyProfile();

    /**
     * Изменение своего профиля
     */
    @PUT("me/profile")
    Call<GsonPutProfileResponse> setMyProfile(String name, String hexColor);

    /**
     * Башни текущего пользователя
     */
    @GET("/me/towers")
    Call<GsonMyTowersResponse> getMyTowers();

    /**
     * Получение начальной информации
     */
    @GET("info")
    Call<GsonGameInfoResponse> getGameInfo();

    /**
     * Уведомления текущего пользователя
     */
    @GET("/me/notifications")
    Call<GsonNotificationsResponse> getNotifications();

    /**
     * Башни. Получение списка в конкретной точке
     */
    @GET("towers")
    Call<GsonTowersInfoResponse> getTowersInfo(@Query("lat") double lat, @Query("lng") double lng);

    /**
     * Башни. Информация о башне
     */
    @GET("towers/{towerId}")
    Call<GsonTowerInfoRespomse> getTowerInfo(@Path("towerId") long towerId);

    /**
     * Башни. Создание новой
     *
     * @param lat   широта (обязательно, double)
     * @param lng   долгота (обязательно, double).
     * @param title название (обязательно, от 3 символов)
     */
    @POST("towers")
    @FormUrlEncoded
    Call<GsonCreateTowerResponse> createTower(@Field("lat") double lat, @Field("lng") double lng, @Field("title") String title);


    /**
     * Башни. Обновление
     */
    @PUT("towers/{towerId}")
    Call<GsonUpdateTowerResponse> updateTower(@Path("towerId") long towerId, @Query("action") TowerUpdateAction action);

    /**
     * Башни. Битва. Атаковать
     */
    @POST("towers/{towerId}/battle?action=start")
    @FormUrlEncoded
    Call<GsonAttackResponse> attack(@Path("towerId") long towerId);

    /**
     * Башни. Битва. Отступить
     */
    @POST("towers/{towerId}/battle?action=stop&win=false")
    @FormUrlEncoded
    Call<GsonRetreatResponse> retreat(@Path("towerId") long towerId);

    /**
     * Статистика. Топ игроков
     */
    @GET("top")
    Call<GsonGameStatTopResponse> getGameStat(@Query("mode") GameStatMode mode);

    class Builder {
        public static TowersGameApi createInstance(String apiBaseUrl) {

            OkHttpClient.Builder builder = new OkHttpClient.Builder();

            Interceptor auth = chain -> {
                Request original = chain.request();
                String token = prefs().getAccessToken();
                if (token != null) {
                    Request request = chain.request();
                    HttpUrl url = request.url().newBuilder().addQueryParameter("token", token).build();
                    request = request.newBuilder().url(url).build();
                    return chain.proceed(request);
                } else {
                    return chain.proceed(original);
                }
            };

            builder.addInterceptor(auth);

            if (Logger.LOG_API) {
                HttpLoggingInterceptor logger = new HttpLoggingInterceptor(Logger.createApiLogger());
                logger.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(logger);
            }
            OkHttpClient client = builder.build();

            Gson gson = new GsonBuilder()
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(apiBaseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            return retrofit.create(TowersGameApi.class);
        }
    }
}
