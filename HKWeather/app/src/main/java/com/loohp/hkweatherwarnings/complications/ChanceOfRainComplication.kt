package com.loohp.hkweatherwarnings.complications

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.RangedValueComplicationData.Companion.TYPE_PERCENTAGE
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.loohp.hkweatherwarnings.MainActivity
import com.loohp.hkweatherwarnings.R
import com.loohp.hkweatherwarnings.Section
import com.loohp.hkweatherwarnings.shared.Shared
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ThreadLocalRandom

class ChanceOfRainComplication : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val chanceOfRain = 40F
        val icon = Icon.createWithResource(this, R.mipmap.umbrella)
        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                    contentDescription = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                    contentDescription = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                )
                    .setTapAction(null)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = chanceOfRain,
                    min = 0F,
                    max = 100F,
                    contentDescription = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                )
                    .setTapAction(null)
                    .setValueType(TYPE_PERCENTAGE)
                    .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                    .build()
            }
            else -> null
        }
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        ForkJoinPool.commonPool().execute {
            val weatherInfo = Shared.currentWeatherInfo.getLatestValue(this, ForkJoinPool.commonPool()).orIntermediateValue
            if (weatherInfo == null) {
                listener.onComplicationData(null)
                return@execute
            }
            val chanceOfRain = weatherInfo.chanceOfRain
            val icon = Icon.createWithResource(this, R.mipmap.umbrella)
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("launchSection", Section.MAIN.name)
            val pendingIntent = PendingIntent.getActivity(this, ThreadLocalRandom.current().nextInt(0, Int.MAX_VALUE), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            listener.onComplicationData(
                when (request.complicationType) {
                    ComplicationType.SHORT_TEXT -> {
                        ShortTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                            contentDescription = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    ComplicationType.LONG_TEXT -> {
                        LongTextComplicationData.Builder(
                            text = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                            contentDescription = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                        )
                            .setTapAction(pendingIntent)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    ComplicationType.RANGED_VALUE -> {
                        RangedValueComplicationData.Builder(
                            value = chanceOfRain,
                            min = 0F,
                            max = 100F,
                            contentDescription = PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build(),
                        )
                            .setTitle(PlainComplicationText.Builder(String.format("%.0f", chanceOfRain).plus("%")).build())
                            .setTapAction(pendingIntent)
                            .setValueType(TYPE_PERCENTAGE)
                            .setMonochromaticImage(MonochromaticImage.Builder(icon).setAmbientImage(icon).build())
                            .build()
                    }
                    else -> null
                }
            )
        }
    }

}