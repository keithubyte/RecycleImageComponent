package com.keith.recycle.image;

import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.View;

import java.util.HashSet;
import java.util.List;

/**
 * Created by keith on 15/8/23.
 */
public class FixedGridLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "FixedGridLayoutManager";

    private static final int DEFAULT_COUNT = 1;

    /* View Removal Constants */
    private static final int REMOVE_VISIBLE = 0;
    private static final int REMOVE_INVISIBLE = 1;

    /* View Direction Constants */
    private static final int DIRECTION_NONE = -1;
    private static final int DIRECTION_START = 0;
    private static final int DIRECTION_END = 1;
    private static final int DIRECTION_UP = 2;
    private static final int DIRECTION_DOWN = 3;

    /* First (top-left) position visible at any point */
    private int mFirstVisiblePosition;

    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;

    /* Number of columns that exist in the grid */
    private int mTotalColumnCount = DEFAULT_COUNT;

    /* Metrics for the visible window of our data */
    private int mVisibleColumnCount;
    private int mVisibleRowCount;

    /* Flag to force current scroll offests to be ignored on re-layout */
    private boolean mForceClearOffsets;

    /* Used for tracking off-screen change events */
    private int mFirstChangedPosition;
    private int mChangedPositionCount;

    /**
     * Set the number of columns the layout manager will use.
     * This will trigger a layout update.
     * @param count Number of columns.
     */
    public void setTotalColumnCount(int count) {
        mTotalColumnCount = count;
        requestLayout();
    }

    /**
     * You must return true from this method if you want your
     * LayoutManager to support anything beyond "simple" item
     * animations. Enabling this causes onLayoutChildren() to
     * be called twice on each animated change; once for a
     * pre-layout, and again for the real layout.
     * @return
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return true;
    }

    /**
     * Called by RecyclerView when a view removal is triggered. This is called
     * before onLayoutChildren() in pre-layout if the views removed are not
     * visible. We use it in this case to inform pre-layout that a removal took place.
     *
     * This method is still called if the views removed were visible, but it will happen
     * AFTER pre-layout.
     * @param recyclerView
     * @param positionStart
     * @param itemCount
     */
    @Override
    public void onItemsRemoved(RecyclerView recyclerView, int positionStart, int itemCount) {
        mFirstChangedPosition = positionStart;
        mChangedPositionCount = itemCount;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        // We have nothing to show for an empty data set but clear any existing views
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }

        if (getChildCount() == 0 && state.isPreLayout()) {
            // Nothing to do during pre-layout when empty
            return;
        }

        // Clear change tracking state when a real layout occurs
        if (!state.isPreLayout()) {
            mFirstChangedPosition = mChangedPositionCount = 0;
        }

        if (getChildCount() == 0) { // First or empty layout
            // Scrap measure one child
            View scrap = recycler.getViewForPosition(0);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);

            /**
             *  We make some assumptions in this code based on every child
             *  view being the same size (i.e, a uniform grid). This allows
             *  us to compute the following values up front because they
             *  won't change.
             */
            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);

            detachAndScrapView(scrap, recycler);
        }

        // Always update the visible row/column counts
        updateWindowSizing();

        SparseIntArray removedCache = null;

        /**
         * During pre-layout, we need to take note of any views that are
         * being removed in order to handle predictive animations
         */
        if (state.isPreLayout()) {
            removedCache = new SparseIntArray(getChildCount());
            for (int i = 0; i < getChildCount(); i++) {
                final View view = getChildAt(i);
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (lp.isItemRemoved()) {
                    // Track these view removals as visible
                    removedCache.put(lp.getViewLayoutPosition(), REMOVE_VISIBLE);
                }
            }

            // Track view removals that happened out of bounds (i.e, off-screen)
            if (removedCache.size() == 0 && mChangedPositionCount > 0) {
                for (int i = mFirstChangedPosition; i < (mFirstChangedPosition + mChangedPositionCount); i++) {
                    removedCache.put(i, REMOVE_INVISIBLE);
                }
            }
        }

        int childLeft;
        int childTop;
        if (getChildCount() == 0) { // First or empty layout
            // Reset the visible and scroll positions
            mFirstVisiblePosition = 0;
            childLeft = childTop = 0;
        } else if (!state.isPreLayout() && getVisibleChildCount() >= state.getItemCount()) {
            // Data set is too small to scroll fully, just reset position
            mFirstVisiblePosition = 0;
            childLeft = childTop = 0;
        } else { // Adapter data set changes
            /**
             * keep the existing initial position, and save off
             * the current scrolled offset.
             */
            final View topChild = getChildAt(0);
            if (mForceClearOffsets) {
                childLeft = childTop = 0;
                mForceClearOffsets = false;
            } else {
                childLeft = getDecoratedLeft(topChild);
                childTop = getDecoratedTop(topChild);
            }
        }

        /**
         * When data set is too small to scroll vertically, adjust vertical offset
         * and shift position to the first row, preserving current column.
         */
        if (!state.isPreLayout() && getVerticalSpace() > (getTotalRowCount() * mDecoratedChildHeight)) {
            mFirstVisiblePosition = mFirstVisiblePosition % getTotalColumnCount();
            childTop = 0;

            // If the shift overscrolls the column max, back it off
            if ((mFirstVisiblePosition + mVisibleColumnCount()) > state.getItemCount()) {
                mFirstVisiblePosition = Math.max(state.getItemCount() - mVisibleColumnCount, 0);
                childLeft = 0;
            }
        }

        /**
         * Adjust the visible position if out of bounds in the new layout.
         * This occurs when the new item count in an adapter is much smaller
         * than it was before, and you are scrolled to a location where no
         * items would exist.
         */
        int maxFirstRow = getTotalRowCount() - (mVisibleRowCount - 1);
        int maxFirstCol = getTotalColumnCount() - (mVisibleColumnCount - 1);
        boolean isOutOfRowBounds = getFirstVisibleRow() > maxFirstRow;
        boolean isOutOfColumnBounds = getFirstVisibleColumn() > maxFirstCol;

        if (isOutOfRowBounds || isOutOfColumnBounds) {
            int firstRow;
            if (isOutOfRowBounds) {
                firstRow = maxFirstRow;
            } else {
                firstRow = getFirstVisibleRow();
            }

            int firstColumn;
            if (isOutOfColumnBounds) {
                firstColumn = maxFirstCol;
            } else {
                firstColumn = getFirstVisibleColumn();
            }

            mFirstVisiblePosition = firstRow * getTotalColumnCount() + firstColumn;

            childLeft = getHorizontalSpace() - (mDecoratedChildWidth * mVisibleColumnCount);
            childTop = getVerticalSpace() - (mDecoratedChildHeight * mVisibleRowCount);

            // Correct cases where shifting to the bottom-right overscrolls the top-left
            // This happens on data sets too small to scroll in a direction.
            if (getFirstVisibleRow() == 0) {
                childTop = Math.min(childTop, 0);
            }
            if (getFirstVisibleColumn() == 0) {
                childLeft = Math.min(childLeft, 0);
            }
        }

        // clear all attached views into the recycler bin
        detachAndScrapAttachedViews(recycler);

        // Fill the grid for the initial layout of views
        fillGrid(DIRECTION_NONE, childLeft, childTop, recycler, state, removedCache);

        // Evaluate any disappearing views that may exist
        if (!state.isPreLayout() && !recycler.getScrapList().isEmpty()) {
            final List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
            final HashSet<View> disappearingViews = new HashSet<>(scrapList.size());

            for (RecyclerView.ViewHolder holder : scrapList) {
                final View child = holder.itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
                if (!lp.isItemRemoved()) {
                    disappearingViews.add(child);
                }
            }

            for (View child : disappearingViews) {
                layoutDisappearingView(child);
            }
        }

    }

    @Override
    public void onAdapterChanged(RecyclerView.Adapter oldAdapter, RecyclerView.Adapter newAdapter) {
        // Completely scrap the existing layout
        removeAllViews();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return null;
    }
}
