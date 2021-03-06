package com.events.calendar.views

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Parcel
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.events.calendar.R
import com.events.calendar.adapters.MonthsAdapter
import com.events.calendar.adapters.WeeksAdapter
import com.events.calendar.utils.EventDots
import com.events.calendar.utils.Events
import com.events.calendar.utils.EventsCalendarUtil
import java.util.*

class EventsCalendar : ViewPager, MonthView.Callback {

    companion object {
        const val WEEK_MODE = 0
        const val MONTH_MODE = 1
    }

    lateinit var mMinMonth: Calendar
    lateinit var mMaxMonth: Calendar

    private var mAttrs: AttributeSet? = null
    private var mCurrentItem: MonthView? = null
    private var mCurrentItemHeight: Int = 0
    private var mCallback: Callback? = null
    private var mCalendarMonthsAdapter: MonthsAdapter? = null
    private var doChangeAdapter: Boolean = false
    private var mCalendarWeekPagerAdapter: WeeksAdapter? = null
    private var mSelectedMonthPosition: Int = 0
    private var mSelectedWeekPosition: Int = 0
    private var doFocus = true

    val weekStartDay: Int get() = EventsCalendarUtil.weekStartDay
    var isPagingEnabled = true //Boolean used to switch off and on EventsCalendar's page change

    val visibleContentHeight: Float
        get() {
            val resources = resources
            return resources.getDimension(R.dimen.height_month_title) + resources.getDimension(R.dimen.height_week_day_header) + resources.getDimension(R.dimen.dimen_date_text_view)
        }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        mAttrs = attrs

        with(context.obtainStyledAttributes(attrs, R.styleable.EventsCalendar, 0, 0)) {
            try {
                EventsCalendarUtil.apply {
                    primaryTextColor = getColor(R.styleable.EventsCalendar_primaryTextColor, Color.BLACK)
                    secondaryTextColor = getColor(R.styleable.EventsCalendar_secondaryTextColor, ContextCompat.getColor(context, R.color.text_black_disabled))
                    selectedTextColor = getColor(R.styleable.EventsCalendar_selectedTextColor, Color.WHITE)
                    selectionColor = getColor(R.styleable.EventsCalendar_selectionColor, EventsCalendarUtil.primaryTextColor)
                    eventDotColor = getColor(R.styleable.EventsCalendar_eventDotColor, EventsCalendarUtil.eventDotColor)
                    monthTitleColor = getColor(R.styleable.EventsCalendar_monthTitleColor, EventsCalendarUtil.secondaryTextColor)
                    weekHeaderColor = getColor(R.styleable.EventsCalendar_weekHeaderColor, EventsCalendarUtil.secondaryTextColor)
                    isBoldTextOnSelectionEnabled = getBoolean(R.styleable.EventsCalendar_isBoldTextOnSelectionEnabled, false)
                }
            } finally {
                recycle()
            }
        }


        EventsCalendarUtil.currentMode = EventsCalendarUtil.MONTH_MODE
        EventsCalendarUtil.setCurrentSelectedDate(Calendar.getInstance())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        var startMonth = Calendar.getInstance()
        var endMonth = Calendar.getInstance()

        if (mCallback != null) {
            startMonth = mMinMonth
            endMonth = mMaxMonth
        } else {
            startMonth.add(Calendar.MONTH, -1)
            endMonth.add(Calendar.MONTH, EventsCalendarUtil.DEFAULT_NO_OF_MONTHS / 2)
        }

        mCalendarMonthsAdapter = MonthsAdapter(this, startMonth, endMonth)
        mCalendarWeekPagerAdapter = WeeksAdapter(this, startMonth, endMonth)

        adapter = mCalendarMonthsAdapter

        mSelectedMonthPosition = EventsCalendarUtil.getMonthPositionForDay(EventsCalendarUtil.getCurrentSelectedDate(), startMonth)
        mSelectedWeekPosition = EventsCalendarUtil.getWeekPosition(EventsCalendarUtil.getCurrentSelectedDate()!!, startMonth)

        currentItem = mSelectedMonthPosition

        addOnPageChangeListener(mOnPageChangeListener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        measureChildren(widthMeasureSpec, heightMeasureSpec)

        try {
            with(adapter as WeeksAdapter) {
                mCurrentItem = if (EventsCalendarUtil.currentMode == EventsCalendarUtil.WEEK_MODE) {
                    getItem(currentItem)
                } else {
                    getItem(currentItem)
                }
            }
            mCurrentItemHeight = mCurrentItem?.measuredHeight!!
        } catch (e: NullPointerException) {
            e.printStackTrace()
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }

        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), mCurrentItemHeight)
    }

    fun setCurrentMonthTranslationFraction(fraction: Float) {
        mCurrentItem?.setMonthTranslationFraction(fraction)
    }

    fun setCalendarMode(mode: Int) {
        if (mode != EventsCalendarUtil.currentMode) {
            EventsCalendarUtil.currentMode = mode
            doChangeAdapter = true
        }
    }

    fun setCallback(callback: Callback) {
        mCallback = callback
    }

    override fun onDaySelected(isClick: Boolean) {
        if (mCallback != null) {
            if (!DatesGridLayout.selectedDateText?.isCurrentMonth!!) {
                val itemNo = if (EventsCalendarUtil.getCurrentSelectedDate()!!.get(Calendar.DATE) < 8) currentItem + 1 else currentItem - 1

                if (itemNo >= 0 && itemNo <= EventsCalendarUtil.getWeekCount(mMinMonth, mMaxMonth)) {
                    setCurrentSelectedDate(EventsCalendarUtil.getCurrentSelectedDate())
                }
            } else {
                if (isClick) {
                    setCurrentSelectedDate(EventsCalendarUtil.getCurrentSelectedDate())
                    mCallback?.onDaySelected(EventsCalendarUtil.getCurrentSelectedDate())
                } else {
                    mCallback?.onMonthChanged(EventsCalendarUtil.getCurrentSelectedDate())
                }
            }
        }
    }

    fun changeAdapter() {
        if (doChangeAdapter) {
            DatesGridLayout.clearSelectedDateTextView()
            val parcel = Parcel.obtain()

            if (Build.VERSION.SDK_INT >= 23) parcel.writeParcelable(null, 0)

            parcel.writeParcelable(null, 0)

            val currentSelectionDate = EventsCalendarUtil.getCurrentSelectedDate()

            adapter = if (EventsCalendarUtil.currentMode == EventsCalendarUtil.WEEK_MODE) {
                val position = EventsCalendarUtil.getWeekPosition(currentSelectionDate, mMinMonth)
                setCurrentItemField(position)
                mCalendarWeekPagerAdapter
            } else {
                val position = EventsCalendarUtil.getMonthPositionForDay(currentSelectionDate, mMinMonth)
                setCurrentItemField(position)
                mCalendarMonthsAdapter
            }

            doChangeAdapter = false
        }
    }

    private fun setCurrentItemField(position: Int) {
        try {
            val field = ViewPager::class.java.getDeclaredField("mRestoredCurItem")//No I18N
            field.isAccessible = true
            field.set(this, position)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

    }

    fun setMonthRange(minMonth: Calendar, maxMonth: Calendar) {
        mMinMonth = minMonth
        mMaxMonth = maxMonth
        Events.initialize(mMinMonth, mMaxMonth)
    }

    fun setPrimaryTextColor(color: Int) {
        EventsCalendarUtil.primaryTextColor = color
    }

    fun setSecondaryTextColor(color: Int) {
        EventsCalendarUtil.secondaryTextColor = color
    }

    fun setEventDotColor(color: Int) {
        EventsCalendarUtil.eventDotColor = color
    }

    fun setSelectedTextColor(color: Int) {
        EventsCalendarUtil.selectedTextColor = color
    }

    fun setSelectionColor(color: Int) {
        EventsCalendarUtil.selectionColor = color
    }

    fun setMonthTitleColor(color: Int) {
        EventsCalendarUtil.monthTitleColor = color
    }

    fun setWeekHeaderColor(color: Int) {
        EventsCalendarUtil.weekHeaderColor = color
    }

    fun setDatesTypeface(typeface: Typeface) {
        EventsCalendarUtil.datesTypeface = typeface
    }

    fun setMonthTitleTypeface(typeface: Typeface) {
        EventsCalendarUtil.monthTitleTypeface = typeface
    }

    fun setWeekHeaderTypeface(typeface: Typeface) {
        EventsCalendarUtil.weekHeaderTypeface = typeface
    }

    fun setIsBoldTextOnSelectionEnabled(isEnabled: Boolean) {
        EventsCalendarUtil.isBoldTextOnSelectionEnabled = isEnabled
    }

    fun addEvent(date: String) {
        Events.add(date)
    }

    fun addEvent(c: Calendar) {
        Events.add(c)
    }

    fun addEvent(arrayOfCalendars: Array<Calendar>) {
        for (c in arrayOfCalendars) {
            addEvent(c)
        }
    }

    fun nextPage(smoothScroll: Boolean = true) {
        setCurrentItem(currentItem + 1, smoothScroll)
    }

    fun previousPage(smoothScroll: Boolean = true) {
        setCurrentItem(currentItem - 1, smoothScroll)
    }

    fun clearEvents() {
        Events.clear()
    }

    fun disableDate(c: Calendar) {
        EventsCalendarUtil.disabledDates.add(c)
    }

    fun disableDaysInWeek(vararg days: Int) {
        EventsCalendarUtil.disabledDays.clear()

        for (day in days) {
            EventsCalendarUtil.disabledDays.add(day)
        }
    }

    fun hasEvent(c: Calendar): Boolean = Events.hasEvent(c)

    fun getDotsForMonth(monthCalendar: Calendar): EventDots? = Events.getDotsForMonth(monthCalendar)

    fun getDotsForMonth(monthString: String?): EventDots? = Events.getDotsForMonth(monthString)


    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = if (this.isPagingEnabled) super.onInterceptTouchEvent(event) else false

    override fun onTouchEvent(event: MotionEvent): Boolean = if (this.isPagingEnabled) super.onTouchEvent(event) else false

    fun invalidateColors() {
        DateText.invalidateColors()
    }

    private val mOnPageChangeListener = object : ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (childCount > 0) {
                if (doFocus) if (EventsCalendarUtil.currentMode != EventsCalendarUtil.WEEK_MODE) mCalendarMonthsAdapter!!.getItem(position)!!.onFocus(position)
                else doFocus = true
            }
        }
    }

    interface Callback {
        fun onDaySelected(selectedDate: Calendar?)
        fun onMonthChanged(monthStartDate: Calendar?)
    }

    fun setCurrentSelectedDate(selectedDate: Calendar?) {
        val position: Int
        if (isPagingEnabled) {
            doFocus = false

            if (EventsCalendarUtil.currentMode == EventsCalendarUtil.MONTH_MODE) {
                position = EventsCalendarUtil.getMonthPositionForDay(selectedDate, mMinMonth)
                setCurrentItem(position, false)

                if (mCalendarMonthsAdapter != null) {
                    post {
                        EventsCalendarUtil.monthPos = currentItem
                        EventsCalendarUtil.selectedDate = selectedDate
                        mCalendarMonthsAdapter?.getItem(currentItem)?.setSelectedDate(selectedDate!!)
                        doFocus = true
                    }
                }
            } else {
                position = EventsCalendarUtil.getWeekPosition(selectedDate, mMinMonth)
                setCurrentItem(position, false)

                if (mCalendarWeekPagerAdapter != null) {
                    post {
                        mCalendarWeekPagerAdapter?.getItem(currentItem)?.setSelectedDate(selectedDate!!)
                        doFocus = true
                    }
                }
            }
        }
    }

    fun getCurrentSelectedDate(): Calendar? = EventsCalendarUtil.selectedDate

    fun reset() {
        for (i in 0 until childCount) {
            (getChildAt(i) as MonthView).reset(false)
        }
    }

    fun refreshTodayDate() {
        for (i in 0 until childCount) {
            (getChildAt(i) as MonthView).refreshDates()
        }
    }

    fun setToday(c: Calendar) {
        EventsCalendarUtil.today = c
        EventsCalendarUtil.setCurrentSelectedDate(c)
    }

    fun setWeekStartDay(weekStartDay: Int, doReset: Boolean) {
        EventsCalendarUtil.weekStartDay = weekStartDay

        if (doReset) {
            mSelectedMonthPosition = EventsCalendarUtil.getMonthPositionForDay(EventsCalendarUtil.getCurrentSelectedDate(), mMinMonth)
            mSelectedWeekPosition = EventsCalendarUtil.getWeekPosition(EventsCalendarUtil.getCurrentSelectedDate()!!, mMinMonth)
            doChangeAdapter = true
            changeAdapter()
            mCallback?.onDaySelected(EventsCalendarUtil.getCurrentSelectedDate())
        }
    }

}
