package ru.mail.my.towers.ui;


import java.util.Collections;
import java.util.List;

import ru.mail.my.towers.toolkit.ThreadPool;

public abstract class PagedDataSource<T> implements AsyncDataSource<T> {

    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final int DEFAULT_PREFETCH_THRESHOLD = 4;

    private final int pageSize;
    private final int prefetchThreshold;
    private List<T> mActivePage = Collections.emptyList();
    private int mActivePageIndex = -1;
    private List<T> mShadowPage = Collections.emptyList();
    private int mShadowPageIndex = -1;
    private volatile int mRequestedPage = -1;

    protected PagedDataSource() {
        this(DEFAULT_PAGE_SIZE, DEFAULT_PREFETCH_THRESHOLD);
    }

    PagedDataSource(int pageSize, int prefetchThreshold) {
        this.pageSize = pageSize;
        this.prefetchThreshold = prefetchThreshold;
    }

    @Override
    public void requestData() {
        prefetch(0);
    }

    @Override
    public T get(int index) {
        final int requestedPageIndex = index / pageSize;

        if (requestedPageIndex == mActivePageIndex) {
            int offset = index % pageSize;
            if (offset < prefetchThreshold && mShadowPageIndex != (requestedPageIndex - 1)) {
                prefetch(requestedPageIndex - 1);
            } else if (offset > (pageSize - prefetchThreshold) && mShadowPageIndex != (requestedPageIndex + 1)) {
                prefetch(requestedPageIndex + 1);
            }
            return mActivePage.get(offset);
        }

        if (requestedPageIndex == mShadowPageIndex) {
            swapPages();
            return get(index);
        }

        prefetchSync(requestedPageIndex);
        return get(index);
    }

    private synchronized void swapPages() {
        int ti = mShadowPageIndex;
        mShadowPageIndex = mActivePageIndex;
        mActivePageIndex = ti;

        List<T> tl = mShadowPage;
        mShadowPage = mActivePage;
        mActivePage = tl;
    }

    private void prefetch(final int pageIndex) {
        if (mRequestedPage == pageIndex)
            return;
        mRequestedPage = pageIndex;

        ThreadPool.DB.execute(() -> prefetchSync(pageIndex));
    }

    private synchronized void prefetchSync(int pageIndex) {
        if (mShadowPageIndex != pageIndex) {
            List<T> list = prepareDataSync(pageIndex * pageSize, pageSize);

            mShadowPageIndex = pageIndex;
            mShadowPage = list;
        }
        mRequestedPage = -1;
    }

    protected abstract List<T> prepareDataSync(int skip, int limit);
}
