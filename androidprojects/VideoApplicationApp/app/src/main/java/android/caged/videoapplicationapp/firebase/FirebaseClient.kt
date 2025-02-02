package android.caged.videoapplicationapp.firebase

import android.caged.videoapplicationapp.model.Data
import android.caged.videoapplicationapp.utils.Status
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import okhttp3.internal.wait
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseClient @Inject constructor(
    private val databaseReference: DatabaseReference, private val gson: Gson
) {
    private var currentUserName: String? = null


     fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        databaseReference.addListenerForSingleValueEvent(object : DatabaseEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(username)) {
                    val dbPassword =
                        snapshot.child(username).child(FirebaseFieldNames.PASSWORD).value.toString()
                    if (dbPassword == password) {
                        databaseReference.child(username).child(FirebaseFieldNames.STATUS).setValue(
                            Status.ONLINE
                        ).addOnCompleteListener {
                                currentUserName = username
                                done(true, null)
                            }.addOnFailureListener {
                                done(false, it.message)
                            }
                    } else {
                        done(false, "Incorrect password")
                    }
                } else {
                    try {
                        databaseReference.child(username).child(FirebaseFieldNames.PASSWORD)
                            .setValue(password).addOnCompleteListener {
                                databaseReference.child(username).child(FirebaseFieldNames.STATUS)
                                    .setValue(Status.ONLINE).addOnCompleteListener {
                                        currentUserName = username
                                        done(true, null)
                                    }.addOnFailureListener {
                                        done(false, it.message)
                                    }
                            }.addOnFailureListener {
                                done(false, it.message)
                            }.addOnFailureListener {
                                done(false, it.message)
                            }
                    } catch (e: Exception) {
                        done(false, e.printStackTrace().toString().substring(0, 10) + "....")
                    }
                }
            }
        })
    }

    fun observeUserStatus(status : (List<Pair<String,String>>) -> Unit) {
        databaseReference.addValueEventListener(object : DatabaseEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.filter {
                    it.key != currentUserName
                }.map {
                    it.key!! to it.child(FirebaseFieldNames.STATUS).value.toString()
                }
                status(users)
            }
        })
    }

    fun logout() {
        currentUserName?.let {
            databaseReference.child(it).child(FirebaseFieldNames.STATUS).setValue(Status.OFFLINE)
        }
    }

    fun subscribeForLatestEvent(listener: Listener) {
        if (currentUserName != null) {
            try {
                val path = databaseReference.child(currentUserName!!).child(FirebaseFieldNames.LATEST_EVENT)
                Log.i("FirebaseClient", "Subscribing to path: $path")

                path.addValueEventListener(object : DatabaseEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val event = try {
                            gson.fromJson(snapshot.value.toString(), Data::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseClient", "Error parsing event: ${e.message}")
                            e.printStackTrace()
                            null
                        }
                        Log.i("FirebaseClient", "Event received: $event")
                        event?.let {
                            listener.onLatestEventReceived(it)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseClient", "Error subscribing to latest event: ${error.message}")
                    }
                })
            } catch (e: Exception) {
                Log.e("FirebaseClient", "Exception during subscribeForLatestEvent: ${e.message}")
                e.printStackTrace()
            }
        } else {
            Log.e("FirebaseClient", "Current user name is null. Cannot subscribe to latest event.")
        }
    }



    fun sendMessageToOtherClient(message: Data, success: (Boolean) -> Unit) {
        Log.i("FirebaseClient", "CurrentUser: $currentUserName")
        val convertedMessage = gson.toJson(message.copy(sender = currentUserName))
        databaseReference.child(message.target!!).child(FirebaseFieldNames.LATEST_EVENT)
            .setValue(convertedMessage).addOnCompleteListener {
                success(true)
            }.addOnFailureListener {
                success(false)
            }
    }

    interface Listener {
        fun onLatestEventReceived(event: Data)
    }
}

object FirebaseFieldNames {
    const val STATUS = "status"
    const val PASSWORD = "password"
    const val LATEST_EVENT = "latest_event"
}