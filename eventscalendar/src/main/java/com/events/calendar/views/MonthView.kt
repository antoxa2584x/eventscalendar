package com.events.calendar.views

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.events.calendar.R
import com.events.calendar.utils.EventsCalendarUtil
import java.text.DateFormatSymbols
import java.util.*

class MonthView : ViewGroup, DatesGridLayout.CallBack {
    private lateinit var mLayoutInflater: LayoutInflater
    private lateinit var mWeekDaysHeader: View
    private lateinit var mFirstDay: TextView
    private lateinit var mSecondDay: TextView
    private lateinit var mThirdDay: TextView
    private lateinit var mFourthDay: TextView
    private lateinit var mFifthDay: TextView
    private lateinit var mSixthDay: TextView
    private lateinit var mSeventhDay: TextView
    private lateinit var mMonthGridContainer: MonthGridContainer
    private lateinit var mCallback: Callback
    private lateinit var mMonthTitleTextView: TextView

    lateinit var gridLayout: DatesGridLayout private set

    private var sWeekStartDay = Calendar.MONDAY
    private var mMonth = 0
    private var mYear = 0
    private var mSelectedWeekNo = 0

    fun reset(doChangeWeekStartDay: Boolean) {
        if (doChangeWeekStartDay) {
            sWeekStartDay = EventsCalendarUtil.weekStartDay
            setWeekdayHeader()
            gridLayout.resetWeekStartDay(sWeekStartDay)
        } else {
            gridLayout.refreshDots()
        }
    }

    fun refreshDates() {
        gridLayout.refreshToday()
    }

    interface Callback {
        fun onDaySelected(isClick: Boolean)
    }

    constructor(context: Context) : super(context) {
        init(-1, -1, sWeekStartDay, 1)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(-1, -1, sWeekStartDay, 1)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(-1, -1, sWeekStartDay, 1)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(-1, -1, sWeekStartDay, 1)
    }

    constructor(context: Context, month: Int, year: Int, weekStartDay: Int, selectedWeekNo: Int) : super(context) {
        init(month, year, weekStartDay, selectedWeekNo)
    }

    constructor(context: Context, month: Calendar, weekStartDay: Int, selectedWeekNo: Int) : super(context) {
        init(month.get(Calendar.MONTH), month.get(Calendar.YEAR), weekStartDay, selectedWeekNo)
    }

    private fun init(month: Int, year: Int, weekStartDay: Int, selectedWeekNo: Int) {
        mLayoutInflater = LayoutInflater.from(context)
        mMonth = month
        mYear = year
        sWeekStartDay = weekStartDay
        mSelectedWeekNo = selectedWeekNo
        initMonthTitle()
        initWeekDayHeader()
        setMonthGridLayout()
    }

    private fun initMonthTitle() {
        mMonthTitleTextView = mLayoutInflater.inflate(R.layout.zmail_layout_month_title, null) as TextView

        val calendar = Calendar.getInstance().apply {
            set(mYear, mMonth, 1)
        }

        mMonthTitleTextView.apply {
            text = EventsCalendarUtil.getMonthString(calendar, EventsCalendarUtil.DISPLAY_STRING)
            setTextColor(EventsCalendarUtil.primaryTextColor)

            if (EventsCalendarUtil.monthTitleTypeface != null) {
                typeface = EventsCalendarUtil.monthTitleTypeface
            }

            setTextColor(EventsCalendarUtil.monthTitleColor)

            addView(this)
        }
    }

    private fun initWeekDayHeader() {
        mWeekDaysHeader = mLayoutInflater.inflate(R.layout.zmail_layout_weekday_header, null)

        with(mWeekDaysHeader) {
            mFirstDay = findViewById(R.id.first_day)
            mSecondDay = findViewById(R.id.second_day)
            mThirdDay = findViewById(R.id.third_day)
            mFourthDay = findViewById(R.id.fourth_day)
            mFifthDay = findViewById(R.id.fifth_day)
            mSixthDay = findViewById(R.id.sixth_day)
            mSeventhDay = findViewById(R.id.seventh_day)
        }

        setWeekdayHeader()
        addView(mWeekDaysHeader)
    }

    private fun setWeekdayHeader() {
        val headers = arrayOf(mFirstDay, mSecondDay, mThirdDay, mFourthDay, mFifthDay, mSixthDay, mSeventhDay)

        for (i in 0..6) {
            setWeekDayHeaderString(headers[i], if (i + sWeekStartDay > 7) (i + sWeekStartDay) % 7 else i + sWeekStartDay)
        }
    }

    private fun setWeekDayHeaderString(header: TextView, calendarConstant: Int) {
        header.setTextColor(EventsCalendarUtil.weekHeaderColor)

        if (EventsCalendarUtil.weekHeaderTypeface != null) {
            header.typeface = EventsCalendarUtil.weekHeaderTypeface
        }

        val namesOfDays = DateFormatSymbols.getInstance().shortWeekdays

        when (calendarConstant) {
            Calendar.SUNDAY -> header.text = namesOfDays[Calendar.SUNDAY].toUpperCase()
            Calendar.MONDAY -> header.text = namesOfDays[Calendar.MONDAY].toUpperCase()
            Calendar.TUESDAY -> header.text = namesOfDays[Calendar.TUESDAY].toUpperCase()
            Calendar.WEDNESDAY -> header.text = namesOfDays[Calendar.WEDNESDAY].toUpperCase()
            Calendar.THURSDAY -> header.text = namesOfDays[Calendar.THURSDAY].toUpperCase()
            Calendar.FRIDAY -> header.text = namesOfDays[Calendar.FRIDAY].toUpperCase()
            Calendar.SATURDAY -> header.text = namesOfDays[Calendar.SATURDAY].toUpperCase()
        }
    }

    private fun setMonthGridLayout() {
        gridLayout = DatesGridLayout(context, mMonth, mYear, sWeekStartDay, mSelectedWeekNo).apply {
            setCallback(this@MonthView)
        }

        mMonthGridContainer = MonthGridContainer(context, gridLayout)

        addView(mMonthGridContainer)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mMonthTitleTextView.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(resources.getDimension(R.dimen.height_month_title).toInt(), View.MeasureSpec.EXACTLY))
        mWeekDaysHeader.measure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(resources.getDimension(R.dimen.height_week_day_header).toInt(), View.MeasureSpec.EXACTLY))
        mMonthGridContainer.measure(widthMeasureSpec, heightMeasureSpec)

        val height = mMonthTitleTextView.measuredHeight + mWeekDaysHeader.measuredHeight + mMonthGridContainer.measuredHeight

        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mMonthTitleTextView.layout(0, 0, mMonthTitleTextView.measuredWidth, mMonthTitleTextView.measuredHeight)

        mWeekDaysHeader.layout(0, mMonthTitleTextView.measuredHeight, mWeekDaysHeader.measuredWidth,
                mMonthTitleTextView.measuredHeight + mWeekDaysHeader.measuredHeight)

        mMonthGridContainer.layout(0, mMonthTitleTextView.measuredHeight + mWeekDaysHeader.measuredHeight,
                mMonthGridContainer.measuredWidth,
                mMonthTitleTextView.measuredHeight + mWeekDaysHeader.measuredHeight + mMonthGridContainer.measuredHeight)
    }

    fun setMonthTranslationFraction(fraction: Float) {
        gridLayout.setTranslationFraction(fraction)
    }

    override fun onDaySelected(date: Calendar?, isClick: Boolean) {
        mCallback.onDaySelected(isClick)
    }

    fun setCallback(callBack: Callback) {
        mCallback = callBack
    }

    fun onFocus(pos: Int) {
        if (pos == EventsCalendarUtil.monthPos) {
            gridLayout.selectDefaultDate(EventsCalendarUtil.selectedDate.get(Calendar.DAY_OF_MONTH))
        } else {
            gridLayout.selectDefaultDateOnPageChanged(EventsCalendarUtil.tobeSelectedDate, false)
        }
    }

    fun setSelectedDate(date: Calendar) {
        gridLayout.selectDate(date)
    }
}
