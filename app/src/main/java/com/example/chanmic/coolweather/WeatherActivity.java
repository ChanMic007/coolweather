package com.example.chanmic.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chanmic.coolweather.gson.Forecast;
import com.example.chanmic.coolweather.gson.Weather;
import com.example.chanmic.coolweather.util.HttpUtil;
import com.example.chanmic.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;

    private Button navButton;

    public SwipeRefreshLayout swipeRefresh;

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= 21){// Android 5.0 以上支持执行此代码
            View decorView = getWindow().getDecorView();
            //  活动的布局会显示在状态栏上
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |  View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            // 将状态栏设置成透明色
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }



        setContentView(R.layout.activity_weather);
        //初始化各个控件
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);


        weatherLayout =(ScrollView) findViewById(R.id.weather_layout);

        titleCity = (TextView) findViewById(R.id.title_city);

        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);

        degreeText = (TextView) findViewById(R.id.degree_text);

        weatherInfoText = (TextView)findViewById(R.id.weather_info_text);

        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);

        aqiText = (TextView)findViewById(R.id.aqi_text);

        pm25Text = (TextView) findViewById(R.id.pm25_text);

        comfortText = (TextView) findViewById(R.id.comfort_text);

        carWashText = (TextView) findViewById(R.id.car_wash_text);

        sportText = (TextView) findViewById(R.id.sport_text);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        navButton = (Button) findViewById(R.id.nav_button);


        // 这句解释？答：数据库
        // 服务器请求之后将数据装载到 简单的数据存储（SharedPreferences.Editor）中，在此处获取得到天气数据
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //去缓存查看是否有照片URL，否则则去服务器请求
        String bingPic = prefs.getString("bing_pic",null);
        if(bingPic != null ){
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        String weatherString = prefs.getString("weather",null);

        final String weatherId;


    if(weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else { //第一次先去服务器请求数据
            // 无缓存时候去直接解析天气数据(weather_id 其实是 city_id)
            // 从Intent中取出天气ID，Intent 中的天气ID是哪里来的？
            // 答：从用户界面点击处获取得到
           weatherId = getIntent().getStringExtra("weather_id");

           weatherLayout.setVisibility(View.INVISIBLE);

           requestWeather(weatherId);

        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });

/*
*
*  点击事件，获得DrawerLayout 和 Button 的实例，在点击事件上面加上 openDrawer() 方法
*
* */
    navButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    });

    }

/*
* 处理并展示 Weather 实体类的数据中的数据
* 显示天气信息
*
*
*
* */
    public void showWeatherInfo(Weather weather){

        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "C";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        //获取 Json 内部天气信息
        for (Forecast forecast:weather.forecastList){
        // 将数据加载到各自的 布局控件 中
            View view  = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dataText = (TextView) view.findViewById(R.id.data_text);
            TextView infoText =  (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dataText.setText(forecast.data);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }


        if(weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }


        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);

    }






/*
*
* 根据天气ID请求城市天气信息
*
*
* */
    public void requestWeather(final String weatherId){


        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=9c2dd30d5ad6423ab77ef5fdd7479581";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }





                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if( weather != null  && "ok".equals(weather.status) ){
                            // 服务器请求之后将数据装载到 简单的数据存储（SharedPreferences.Editor）中，在此处存储请求响应得到的天气数据
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }  else {
                            Toast.makeText(WeatherActivity.this, "获取信息失败", Toast.LENGTH_SHORT).show();
                        }

                        swipeRefresh.setRefreshing(false);



                    }
                });
            }
        });
//  在请求天气的方法里，请求背景图片信息，每次请求天气时候都会更新图片信息
        loadBingPic();



    }

    /*
    *
    * 加载每日一图
    *
    * */

    private void loadBingPic(){

        String requestBingPic = "http://guolin.tech/api/bing_pic";

        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                //将获得的图片地址加入缓存
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }



}
