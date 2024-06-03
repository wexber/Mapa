package com.example.mapa

import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import android.Manifest
import android.content.Context
import android.location.Location
import android.widget.Toast
import android.location.Criteria
import android.location.LocationManager
import android.util.Log
import java.io.IOException
import android.location.Address
import android.location.Geocoder
import android.os.Build
//import androidx.compose.ui.text.intl.Locale
import androidx.core.app.ActivityCompat
import com.google.android.libraries.places.api.Places
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import java.util.Timer
import java.util.TimerTask
import android.speech.tts.TextToSpeech
import java.util.Locale



class MainActivity : FragmentActivity(),OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,GoogleMap.OnMyLocationClickListener{
    private lateinit var map:GoogleMap
    private val TAG = "MainActivity"
    private val timer = Timer()
    private lateinit var tts: TextToSpeech

    companion object{
        const val Request_Code_location=0
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val INTERVAL_TIME = 5000L
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.mainactivity)
        // Inicializa el TextToSpeech
        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                // Configura el idioma para la voz
                tts.language = Locale.getDefault()
            }
        }
        createFragment()
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBOad-LsjPBURFajGqGkReCQIoy9Y9aBMU")
        }
            // Solicitar permiso de ubicación si aún no se ha concedido
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Si los permisos no están concedidos, solicitarlos al usuario
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {

                obtenerLugaresCercanos()
            }
        }




    // Método para inicializar y utilizar la API de Places
    private fun inicializarPlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyBOad-LsjPBURFajGqGkReCQIoy9Y9aBMU")
        }
        // Solicitar la ubicación actual y lugares cercanos
        obtenerLugaresCercanos()
    }

    // Método para iniciar la actualización periódica de la dirección
    private fun startLocationUpdates() {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // Obtener la ubicación actual y llamar a la función para obtener la dirección
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val provider: String? = locationManager.getBestProvider(Criteria(), true)
                val location: Location? = provider?.let { locationManager.getLastKnownLocation(it) }
                location?.let {
                    getAddressFromLocation(it.latitude, it.longitude)
                }
            }
        }, 0, INTERVAL_TIME) // INTERVAL_TIME es el tiempo en milisegundos entre cada actualización
    }

    // Método para detener la actualización periódica de la dirección
    private fun stopLocationUpdates() {
        timer.cancel()
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    // Método para obtener la dirección a partir de las coordenadas de ubicación
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare // Nombre de la calle

                // Utiliza TextToSpeech para pronunciar el nombre de la calle
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak("Estás en: $street", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    @Suppress("DEPRECATION")
                    tts.speak("Estás en: $street", TextToSpeech.QUEUE_FLUSH, null)
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la dirección.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error al obtener la dirección: ${e.message}")
            Toast.makeText(this, "Error al obtener la dirección.", Toast.LENGTH_SHORT).show()
        }
    }
/*
    private fun getAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, java.util.Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val street = address.thoroughfare // Nombre de la calle
                runOnUiThread {
                    Toast.makeText(this, "Estás en: $street", Toast.LENGTH_SHORT).show()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "No se pudo obtener la dirección.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error al obtener la dirección: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Error al obtener la dirección.", Toast.LENGTH_SHORT).show()
            }
        }
    }

*/
    private fun obtenerLugaresCercanos() {
        // Crear el cliente de Places
        val placesClient = Places.createClient(this)
        // Definir los campos de lugar que deseas obtener
        val placeFields = listOf(Place.Field.NAME)
        // Obtener la ubicación actual y lugares cercanos
        placesClient.findCurrentPlace(FindCurrentPlaceRequest.newInstance(placeFields)).addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val places = task.result?.placeLikelihoods
                if (places != null) {
                    for (placeLikelihood in places) {
                        val place = placeLikelihood.place
                        Log.i(TAG, "Nombre del lugar: ${place.name}, Probabilidad: ${placeLikelihood.likelihood}")
                    }
                } else {
                    Log.e(TAG, "Error al obtener la ubicación actual.")
                }
            }
        }
    }



    // Método para manejar la respuesta de la solicitud de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el usuario concede los permisos, inicializar y utilizar la API de Places
                inicializarPlaces()
            } else {
                // Si el usuario niega los permisos, mostrar un mensaje o tomar otra acción
                Log.e(TAG, "Permiso de ubicación denegado.")
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun createFragment(){
        val mapFragment : SupportMapFragment =supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map=googleMap
        createMarker()
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        Enablelocation()
    }


   //Metodo para obtener la ubicacion en el mapa En tiempo real
    private fun createMarker(){
        if (isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val criteria = Criteria()
            val provider: String? = locationManager.getBestProvider(criteria, true)
            val location: Location? = provider?.let { locationManager.getLastKnownLocation(it) }
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18f), 4000, null)
                // Llamar a la función para obtener la dirección desde la ubicación
                getAddressFromLocation(it.latitude, it.longitude)
            }
        } else {
            requestlocationPermission()
        }
    }

    private fun isLocationPermissionGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED


    private fun Enablelocation(){
        if(!::map.isInitialized) return
        if(isLocationPermissionGranted()){
            //permiso activado
            map.isMyLocationEnabled= true
            map.setOnMyLocationButtonClickListener(this)
            map.setOnMyLocationClickListener(this)
        }else{
            //no activo los permisos
            requestlocationPermission()

        }
    }
    private fun requestlocationPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "ve ajustes y activa los permisos", Toast.LENGTH_SHORT).show()
        }else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),Request_Code_location)
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if(!::map.isInitialized) return
        if(isLocationPermissionGranted()){
            map.isMyLocationEnabled= false

            Toast.makeText(this,"Para activar la localización ve ajuste y acepta los permisos",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(this,"Estás en ${p0.latitude},${p0.longitude} ",Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener la actualización de la ubicación cuando la actividad se destruye
        stopLocationUpdates()
    }


}



