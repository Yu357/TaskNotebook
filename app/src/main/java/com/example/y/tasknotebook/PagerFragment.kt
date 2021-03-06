package com.example.y.tasknotebook

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.fragment_pager.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class PagerFragment: Fragment() {

    //ページ数
    private val pageSize = Int.MAX_VALUE

    //一か月分の日付情報を格納する配列
    private lateinit var days: Array<LocalDate?>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //GraphFragmentからページ番号を受け取って、当月との差を計算
        val position = arguments?.getInt("position") ?: 0
        val offsetMonth: Int = 0 - (pageSize / 2 - position)

        //一ヵ月分の日付情報を使って、calendarRecyclerViewを表示
        calendarRecyclerView.layoutManager = GridLayoutManager(this.context, 7)
        days = createDays(offsetMonth)
        calendarRecyclerView.adapter = TileRecyclerViewAdapter(days)

        //monthTextを更新
        monthText.text = SimpleDateFormat("yyyy年 M月", Locale.JAPANESE).format(Date().apply { offset(month = offsetMonth) })

        //lineChartを表示
        setLineChart()

    }


    private fun Date.offset(month: Int = 0) {
        time = Calendar.getInstance().apply {
            add(Calendar.MONTH, month)
        }.timeInMillis
    }


    private fun createDays(offsetMonth: Int):Array<LocalDate?>{

        //配列や変数
        val days: MutableList<LocalDate?> = arrayListOf()
        var day: LocalDate = LocalDate.now()
        day = day.plusMonths(offsetMonth.toLong())

        //当月の日数と、一日の曜日を取得
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, offsetMonth)
        val dayOfMonth = calendar.getActualMaximum(Calendar.DATE)
        calendar.set(Calendar.DATE, 1)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        //当月の1日までをnullで埋める
        for (i in 1 until dayOfWeek){
            days.add(null)
        }

        //1日から月末日まで数字を埋める
        for (i in 1..dayOfMonth){
            days.add(LocalDate.of(day.year, day.month, i))
        }

        //余った領域はnullで埋める
        if(days.size > 35){
            val filledSize = (42) - days.size
            for (i in 0 until filledSize){
                days.add(null)
            }
        }else{
            val filledSize = (35) - days.size
            for (i in 0 until filledSize){
                days.add(null)
            }
        }

        //配列daysをreturn
        return days.toTypedArray()
    }


    @SuppressLint("SimpleDateFormat")
    private fun setLineChart(){

        //日別達成数データを格納するリストを宣言
        val achievedData = mutableListOf<Int>()

        //achievedDataにデータを追加
        for (i in days.indices){
            if(days[i] != null){

                //当日の開始日時と終了日時を生成
                val localDate:LocalDate = days[i]!!
                val formatter = SimpleDateFormat("yyyy-MM-dd")
                val startDate: Date = formatter.parse(localDate.toString())!!
                val endDate: Date = formatter.parse(localDate.toString())!!
                endDate.hours = 23
                endDate.minutes = 59
                endDate.seconds = 59

                //当日達成したタスクを全取得
                val realm = Realm.getDefaultInstance()
                val realmResults = realm.where<Task>()
                    .between("achievedDate", startDate, endDate)
                    .findAll()

                //当日のタスク達成数をachievedDataへ追加
                achievedData.add(realmResults.size)
            }
        }

        //1. Entryにデータを格納
        val entryList = mutableListOf<Entry>() //1本目の線
        for(i in achievedData.indices){
            entryList.add(
                Entry(i.toFloat() + 1, achievedData[i].toFloat())
            )
        }

        //LineDataSetのList
        val lineDataSets = mutableListOf<ILineDataSet>()
        //2. DataSetにデータを格納
        val lineDataSet = LineDataSet(entryList, "square")

        //3. この折れ線グラフのスタイル設定
        lineDataSet.color = ContextCompat.getColor(this.requireContext(), R.color.red05)
        lineDataSet.setDrawCircles(false)
        lineDataSet.setDrawValues(false)

        //lineDataSetをリストに格納
        lineDataSets.add(lineDataSet)

        //4. LineDataにLineDataSetを格納
        val lineData = LineData(lineDataSets)
        //5. LineChartにLineDataを格納
        lineChart.data = lineData

        //6. lineChartのスタイル設定
        lineChart.legend.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.setTouchEnabled(false)
        lineChart.extraTopOffset = 5f
        lineChart.xAxis.textColor = ContextCompat.getColor(this.requireContext(), R.color.weak)
        lineChart.xAxis.textSize = 14f
        lineChart.axisLeft.textColor = ContextCompat.getColor(this.requireContext(), R.color.weak)
        lineChart.axisLeft.textSize = 14f
        lineChart.axisLeft.axisMinimum = 0f
        lineChart.axisLeft.granularity = 1f
        lineChart.axisRight.isEnabled = false

        val maxValue = achievedData.maxOrNull() ?: 0
        if(maxValue > 5){
            lineChart.axisLeft.axisMaximum = maxValue.toFloat()
        }else{
            lineChart.axisLeft.axisMaximum = 5f
        }


        //7. LineChart更新
        lineChart.invalidate()

    }


}