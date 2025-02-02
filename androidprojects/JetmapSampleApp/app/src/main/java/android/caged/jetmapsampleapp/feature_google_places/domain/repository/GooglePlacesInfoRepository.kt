package android.caged.jetmapsampleapp.feature_google_places.domain.repository

import android.caged.jetmapsampleapp.util.Resource
import android.caged.jetmapsampleapp.feature_google_places.domain.model.GooglePlacesInfo
import kotlinx.coroutines.flow.Flow

interface GooglePlacesInfoRepository {
    fun getDirection(origin: String, destination: String, key: String): Flow<Resource<GooglePlacesInfo>>
}