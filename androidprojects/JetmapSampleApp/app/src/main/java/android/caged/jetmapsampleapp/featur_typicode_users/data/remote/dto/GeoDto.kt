package android.caged.jetmapsampleapp.featur_typicode_users.data.remote.dto

import android.caged.jetmapsampleapp.featur_typicode_users.domain.model.Geo

data class GeoDto(
    val lat: String,
    val lng: String
){
    fun toGeo(): Geo {
        return Geo(
            lat = lat,
            lng= lng
        )
    }
}