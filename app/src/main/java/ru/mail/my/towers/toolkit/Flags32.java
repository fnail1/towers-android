package ru.mail.my.towers.toolkit;

import android.os.Parcel;
import android.os.Parcelable;

public class Flags32 implements Parcelable{
    public int mValue;

    public Flags32() {

    }
    public Flags32(int value) {
        mValue = value;
    }

    protected Flags32(Parcel in) {
        mValue = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Flags32> CREATOR = new Creator<Flags32>() {
        @Override
        public Flags32 createFromParcel(Parcel in) {
            return new Flags32(in);
        }

        @Override
        public Flags32[] newArray(int size) {
            return new Flags32[size];
        }
    };

    public int get() {
        return mValue;
    }

    public boolean get(int mask) {
        return mask == (mValue & mask);
    }

    public void set(int mask, boolean value) {
        if (value)
            mValue |= mask;
        else
            mValue &= ~mask;
    }

    public void set(int value) {
        mValue = value;
    }
}
