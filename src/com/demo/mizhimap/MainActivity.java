package com.demo.mizhimap;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;
import android.widget.Button;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BaiduMap.OnMapClickListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerDragListener;
import com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.InfoWindow.OnInfoWindowClickListener;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.TextOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;

public class MainActivity extends Activity implements OnClickListener {

	private MapView mapView;
	private BaiduMap baiduMap;
	private Button normMapBtn;
	private Button sateMapBtn;
	private Button trafMapBtn;
	private Button heatMapBtn;
	private Button overlayBtn;
	private Button locateBtn;
	
	private Marker marker1;
	private LocationMode curMode;
	private LocationClient locClient;
//	private LocationManager locManager;
	private BitmapDescriptor currentMarker;
	private MapStatusUpdate mapStatusUpdate;
	
//	private String provider;
	private int overlayIndex=0;//标记显示第几个Marker
	private boolean isFirstIn=true; 
	private double latitude,longitude;
	
	//初始化全局bitmap信息（根据资源id创建bitmap描述信息）
	BitmapDescriptor bitmap=BitmapDescriptorFactory.fromResource(R.drawable.icon_marka);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		init();
	}

	private void init() {
		// TODO Auto-generated method stub
		mapView=(MapView)findViewById(R.id.map_view);
		mapView.removeViewAt(1);
		mapStatusUpdate=MapStatusUpdateFactory.zoomTo(16f);
		baiduMap=mapView.getMap();
		baiduMap.setMapStatus(mapStatusUpdate);
		
		normMapBtn=(Button)findViewById(R.id.norm_map_btn);
		sateMapBtn=(Button)findViewById(R.id.sate_map_btn);
		trafMapBtn=(Button)findViewById(R.id.traf_map_btn);
		heatMapBtn=(Button)findViewById(R.id.heat_map_btn);
		overlayBtn=(Button)findViewById(R.id.overlay_btn);
		locateBtn=(Button)findViewById(R.id.locate_btn);
		normMapBtn.setOnClickListener(this);
		sateMapBtn.setOnClickListener(this);
		trafMapBtn.setOnClickListener(this);
		heatMapBtn.setOnClickListener(this);
		overlayBtn.setOnClickListener(this);
		locateBtn.setOnClickListener(this);
		
		normMapBtn.setEnabled(false);
		curMode=LocationMode.NORMAL;
		locateBtn.setText("普通");
		
		//开启图层
		baiduMap.setMyLocationEnabled(true);
		locClient=new LocationClient(this);
		locClient.registerLocationListener(locationListener);
		LocationClientOption option=new LocationClientOption();
		option.setOpenGps(true);
		option.setScanSpan(2000);
		option.setAddrType("all");
		option.setCoorType("gcj02");
		locClient.setLocOption(option);
		locClient.start();
		
		/*locationManager=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
		List<String> providerList=locationManager.getProviders(true);
		if(providerList.contains(LocationManager.GPS_PROVIDER)){
			provider=LocationManager.GPS_PROVIDER;
		}else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
			provider=LocationManager.NETWORK_PROVIDER;
		}else {
			Toast.makeText(this, "No location provider to use", Toast.LENGTH_SHORT).show();
			return;
		}*/
		
		//对marker添加点击事件
		baiduMap.setOnMarkerClickListener(new OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker arg0) {
				// TODO Auto-generated method stub
				if(arg0==marker1){
					final LatLng latLng=arg0.getPosition();
					Toast.makeText(MainActivity.this, latLng.toString(), Toast.LENGTH_SHORT).show();
				}
				return false;
			}
		});
		//地图点击事件
		baiduMap.setOnMapClickListener(new OnMapClickListener() {
			@Override
			public boolean onMapPoiClick(MapPoi arg0) {
				// TODO Auto-generated method stub
				return false;
			}
			@Override
			public void onMapClick(LatLng latLng) {
				// TODO Auto-generated method stub
				displayInfoWindow(latLng);
			}
		});
		//拖拽事件
		baiduMap.setOnMarkerDragListener(new OnMarkerDragListener() {
			@Override
			public void onMarkerDragStart(Marker arg0) {
				// TODO Auto-generated method stub
			}
			@Override
			public void onMarkerDragEnd(Marker arg0) {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "拖拽结束，新位置是："+arg0.getPosition().latitude+","+arg0.getPosition().longitude, Toast.LENGTH_LONG).show();
				reverseGeoCode(arg0.getPosition());
			}
			@Override
			public void onMarkerDrag(Marker arg0) {
				// TODO Auto-generated method stub
			}
		});
	}
	//反地理编码得到地址信息
	private void reverseGeoCode(LatLng latLng){
		GeoCoder geoCoder=GeoCoder.newInstance();
		OnGetGeoCoderResultListener listener=new OnGetGeoCoderResultListener() {
			@Override
			public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
				// TODO Auto-generated method stub
				if(result==null||result.error!=SearchResult.ERRORNO.NO_ERROR){
					Toast.makeText(MainActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG).show();
				}
				Toast.makeText(MainActivity.this, "位置："+result.getAddress(), Toast.LENGTH_LONG).show();
			}
			@Override
			public void onGetGeoCodeResult(GeoCodeResult result) {
				// TODO Auto-generated method stub
				if(result==null||result.error!=SearchResult.ERRORNO.NO_ERROR){
					Toast.makeText(MainActivity.this, "抱歉，未能找到结果", Toast.LENGTH_LONG).show();
				}
			}
		};
		geoCoder.setOnGetGeoCodeResultListener(listener);
		geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(latLng));
//		geoCoder.destroy();
	}
	BDLocationListener locationListener=new BDLocationListener() {
		@Override
		public void onReceiveLocation(BDLocation location) {
			// TODO Auto-generated method stub
			if(location==null||baiduMap==null){
				return;
			}
			MyLocationData locData=new MyLocationData.Builder().accuracy(location.getRadius()).direction(100).latitude(location.getLatitude()).longitude(location.getLongitude()).build();
			baiduMap.setMyLocationData(locData);
			latitude=location.getLatitude();
			longitude=location.getLongitude();
			if(isFirstIn){
				isFirstIn=false;
				LatLng latLng=new LatLng(latitude, longitude);
				MapStatusUpdate mapStatusUpdate=MapStatusUpdateFactory.newLatLng(latLng);
				baiduMap.animateMapStatus(mapStatusUpdate);
				baiduMap.setMyLocationEnabled(false);
				Toast.makeText(getApplicationContext(), location.getAddrStr(),Toast.LENGTH_SHORT).show();
			}
		}
	};
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.norm_map_btn:
			baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
			normMapBtn.setEnabled(false);
			sateMapBtn.setEnabled(true);
			break;
		case R.id.sate_map_btn:
			baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
			sateMapBtn.setEnabled(false);
			normMapBtn.setEnabled(true);
			break;
		case R.id.traf_map_btn:
			if(!baiduMap.isTrafficEnabled()){
				baiduMap.setTrafficEnabled(true);
				trafMapBtn.setText("关闭实时路况");
			}else {
				baiduMap.setTrafficEnabled(false);
				trafMapBtn.setText("打开实时路况");
			}
			break;
		case R.id.heat_map_btn:
			if(!baiduMap.isBaiduHeatMapEnabled()){
				baiduMap.setBaiduHeatMapEnabled(true);
				heatMapBtn.setText("关闭热力图像");
			}else {
				baiduMap.setBaiduHeatMapEnabled(false);
				heatMapBtn.setText("打开热力图像");
			}
			break;
		case R.id.locate_btn:
			switch(curMode){
			case NORMAL:
				locateBtn.setText("跟随");
				curMode=LocationMode.FOLLOWING;
				break;
			case FOLLOWING:
				locateBtn.setText("罗盘");
				curMode=LocationMode.COMPASS;
				break;
			case COMPASS:
				locateBtn.setText("普通");
				curMode=LocationMode.NORMAL;
				break;
			}
			baiduMap.setMyLocationConfigeration(new MyLocationConfiguration(curMode, true, currentMarker));
			break;
		case R.id.overlay_btn:
			switch(overlayIndex){
			case 0:
				overlayBtn.setText("显示文字Marker");
				addMarkerOverlay();
				break;
			case 1:
				overlayBtn.setText("显示圆点Marker");
				addTextOptions();
				break;
			case 2:
				overlayBtn.setText("显示标注Marker");
				addDotOptions();
				break;
			}
			overlayIndex=(overlayIndex+1)%3;
			break;
		}
	}
	private void addMarkerOverlay() {
		// TODO Auto-generated method stub
		baiduMap.clear();
		LatLng point=new LatLng(latitude, longitude);
		OverlayOptions overlayOptions=new MarkerOptions().position(point).icon(bitmap).zIndex(4).draggable(true);
		marker1=(Marker)baiduMap.addOverlay(overlayOptions);
	}
	private void addTextOptions() {
		// TODO Auto-generated method stub
		baiduMap.clear();
		LatLng latLng=new LatLng(latitude,longitude);
		TextOptions textOptions=new TextOptions();
		textOptions.bgColor(0xAAFFFF00).fontSize(28).fontColor(0xFFFF00FF).text("你的位置").position(latLng);
		baiduMap.addOverlay(textOptions);
	}
	private void addDotOptions() {
		// TODO Auto-generated method stub
		baiduMap.clear();
		DotOptions dotOptions = new DotOptions();
		dotOptions.center(new LatLng(latitude, longitude));
		dotOptions.color(0XFFFAA755);
		dotOptions.radius(25);
		baiduMap.addOverlay(dotOptions);
	}
	private void displayInfoWindow(final LatLng latLng) {
		Button btn=new Button(getApplicationContext());
		btn.setBackgroundResource(R.drawable.popup);
		BitmapDescriptor bitmapDescriptor=BitmapDescriptorFactory.fromView(btn);
		OnInfoWindowClickListener infoWindowClickListener=new OnInfoWindowClickListener() {
			@Override
			public void onInfoWindowClick() {
				reverseGeoCode(latLng);
				baiduMap.hideInfoWindow();
			}
		};
		InfoWindow infoWindow=new InfoWindow(bitmapDescriptor, latLng, -46,infoWindowClickListener);
		baiduMap.showInfoWindow(infoWindow);
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mapView.onResume();
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mapView.onPause();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mapView.onDestroy();
		mapView = null;
		baiduMap.setMyLocationEnabled(false);
		locClient.unRegisterLocationListener(locationListener);
		locClient.stop();
		bitmap.recycle();
	}
}
