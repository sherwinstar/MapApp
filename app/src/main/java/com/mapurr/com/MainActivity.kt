package com.mapurr.com


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.MapsInitializer.Renderer
import com.google.android.gms.maps.model.*
import com.mapurr.com.api.ApiManager
import com.mapurr.com.model.PlaceInfoEntry
import com.mapurr.com.model.PlaceResultEntry
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMapsSdkInitializedCallback {
    //定位client
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var map:GoogleMap? = null

    private var currentLocation : Location? = null
    private var mapCenter : LatLng? = null
    //当前定位marker点
    private var currentMarker: Marker? = null
    private var searchView: androidx.appcompat.widget.SearchView? = null
    private var searchResults = mutableListOf<PlaceInfoEntry>()

    val REQUEST_PHOTO_CODE = 3002 //获取权限
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapsInitializer.initialize(applicationContext, Renderer.LATEST, this)
        setContentView(R.layout.activity_main)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment : SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        searchView = findViewById<SearchView>(R.id.search_view)
        searchView?.clearFocus()
        searchView?.onActionViewExpanded()
        val imm = searchView?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchView?.windowToken, 0)
        searchView?.apply {
            onActionViewExpanded()
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    if (!query.isNullOrEmpty()) {
                        if (currentLocation != null)
                            searchLocations(query, currentLocation!!.latitude, currentLocation!!.longitude)
                        else if (mapCenter != null)
                            searchLocations(query, mapCenter!!.latitude, mapCenter!!.longitude)
                    }
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }

            })
        }
    }

    private  fun searchLocations(keyword: String, latitude: Double, longitude: Double) {
        val apiKey = resources.getString(R.string.google_map_key)
        val radius = 3000
        val location = "$latitude,$longitude"
        ApiManager.getHttpApi().getPlaces(keyword, location, apiKey, radius).enqueue(object : Callback<PlaceResultEntry<Array<PlaceInfoEntry>>> {

            override fun onResponse(
                call: Call<PlaceResultEntry<Array<PlaceInfoEntry>>>?,
                response: Response<PlaceResultEntry<Array<PlaceInfoEntry>>>?
            ) {
                if (response == null) {
                    return
                }
                if (response.isSuccessful && response.body() != null) {
                    var result: PlaceResultEntry<Array<PlaceInfoEntry>> = response.body()
                    if (result.status.equals("OK")) {
                        searchResults.clear()
                        searchResults.addAll(result.results!!.toList())
                        map!!.clear()
                        for (place in searchResults) {
                            addMarker(place)
                        }
//                        jokeAdapter!!.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(
                call: Call<PlaceResultEntry<Array<PlaceInfoEntry>>>?,
                t: Throwable?
            ) {
                Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
            Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        val currencyLatLng = LatLng(36.659584, 117.144005)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currencyLatLng, 16f))
        //获取地图中心位置
        googleMap.setOnCameraMoveListener {
            with(googleMap.cameraPosition.target){
                Log.e("地图中心位置","Lat：$latitude，Lng：$longitude")
                mapCenter = googleMap.cameraPosition.target
            }
        }

        val permission = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        requestPermission(permission, REQUEST_PHOTO_CODE)
//        googleMap.isIndoorEnabled = true
        googleMap.isBuildingsEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        fusedLocationProviderClient.requestLocationUpdates(
            LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)//设置高精度
                .setInterval(3000), //3秒一次定位请求
            locationCallback,
            Looper.getMainLooper())
    }
    //定位回调
    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            var location = locationResult.locations.first()
            if (location != null) {
                drawLocationMarker(location, LatLng(location.latitude,location.longitude))
            }
        }
    }

    private fun addMarker(place: PlaceInfoEntry) {
        if (place.geometry == null || place.geometry!!.location == null)
            return
        map?.addMarker(
            MarkerOptions().position(LatLng(place.geometry!!.location!!.lat.toDouble(),
                place.geometry!!.location!!.lng.toDouble()
            )).title(place.name)
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.chat_loc_point))
        )
    }

    @SuppressLint("NewApi")
    private fun drawLocationMarker(location: Location, latLng: LatLng) {
        if (currentLocation == null){//第一次定位画定位marker
            currentMarker = map?.addMarker(
                MarkerOptions()
                .position( latLng).title("Marker")
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_vehicle_location))
            )
            map?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                latLng,14f
            ))
        }else{
            val deltaTime = location.time - currentLocation!!.time
            //有方位精度
            if (location.hasBearingAccuracy()){
                if (deltaTime <= 0){
                    map?.animateCamera(CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(latLng)
                            .zoom(map?.cameraPosition!!.zoom)
                            .bearing(location.bearing)
                            .build()
                    ))
                }else{
                    map?.animateCamera(CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(latLng)
                            .zoom(map?.cameraPosition!!.zoom)
                            .bearing(location.bearing)
                            .build()
                    ), deltaTime.toInt(),null)
                }
                currentMarker?.rotation = 0f
            }else{
                if (deltaTime <= 0){
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,map?.cameraPosition!!.zoom))
                }else{
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,map?.cameraPosition!!.zoom), deltaTime.toInt(), null)
                }
                //设置marker的指针方向
                currentMarker?.rotation = location.bearing - (map?.cameraPosition?.bearing ?:0f)
            }

        }
        currentLocation = location
        Log.e(TAG, "currentLocation="+currentLocation.toString())
    }

    private fun stopLocationUpdates(){
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()

    }
    ///----------
    /**
     * 动态获权
     * */
    /**
     * 动态获权请求值
     */
    private var REQUEST_CODE_PERMISSION = 0x00099
    protected val TAG = this.javaClass.simpleName

    /**
     * 请求权限
     * 动态获权
     * @param permissions 请求的权限
     * @param requestCode 请求权限的请求码
     */
    open fun requestPermission(
        permissions: Array<String>,
        requestCode: Int
    ) {
        REQUEST_CODE_PERMISSION = requestCode
        if (checkPermissions(permissions)) {
            permissionSuccess(REQUEST_CODE_PERMISSION)
        } else {
            try {
                val needPermissions =
                    getDeniedPermissions(permissions)
                ActivityCompat.requestPermissions(
                    this,
                    needPermissions.toTypedArray(),
                    REQUEST_CODE_PERMISSION
                )
            } catch (e: Exception) {
                Log.e("BaseActivity", "获取权限失败 Exception = $e")
            }
        }
    }

    /**
     * 检测所有的权限是否都已授权
     */
    fun checkPermissions(permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * 获取权限集中需要申请权限的列表
     */
    fun getDeniedPermissions(permissions: Array<String>): List<String> {
        val needRequestPermissionList: MutableList<String> =
            ArrayList()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
            ) {
                needRequestPermissionList.add(permission)
            }
        }
        return needRequestPermissionList
    }

    /**
     * 系统请求权限回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (verifyPermissions(grantResults)) {
                permissionSuccess(REQUEST_CODE_PERMISSION)
            } else {
                permissionFail(REQUEST_CODE_PERMISSION)
            }
        }
    }

    /**
     * 确认所有的权限是否都已授权
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        for (grantResult in grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * 获取权限成功
     */
    open fun permissionSuccess(requestCode: Int) {
        Log.e(TAG, "获取权限成功=$requestCode")

        startLocationUpdates()
    }

    /**
     * 权限获取失败
     */
    open fun permissionFail(requestCode: Int) {
        Log.e(TAG, "获取权限失败=$requestCode")
    }

    //-----
    //反向地理编码
    private fun latlngToAddress(lat: Double,lng: Double){
        val geocoder = Geocoder(this)
        try {
            val result = geocoder.getFromLocation(lat,lng,1)
            if (result != null && result.isNotEmpty()){
                val addressName = result[0].featureName
            }
        }catch (e: Exception){

        }
    }

}
