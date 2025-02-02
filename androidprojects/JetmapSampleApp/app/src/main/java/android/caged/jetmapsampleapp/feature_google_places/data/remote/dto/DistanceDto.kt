package android.caged.jetmapsampleapp.feature_google_places.data.remote.dto

import android.caged.jetmapsampleapp.feature_google_places.domain.model.Distance

data class DistanceDto(
    val text: String,
    val value: Int
){
    fun toDistance(): Distance {
        return  Distance(
            text = text,
            value = value
        )
    }
}
