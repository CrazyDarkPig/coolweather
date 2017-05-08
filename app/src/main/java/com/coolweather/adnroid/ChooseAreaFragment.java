package com.coolweather.adnroid;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.adnroid.db.City;
import com.coolweather.adnroid.db.County;
import com.coolweather.adnroid.db.Province;
import com.coolweather.adnroid.util.HttpUtil;
import com.coolweather.adnroid.util.Utility;

import org.litepal.crud.DataSupport;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by dark on 2017/5/4.
 */

public class ChooseAreaFragment extends Fragment {
    public   static  final  int LEVEL_PROVINCE=0;
    public   static  final  int LEVEL_CITY=1;
    public  static final  int LEVEL_COUNTY=2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList=new ArrayList<>();

    private List<Province> provincesList;
    private  List<City> cityList;
    private  List<County> countyList;

    private  Province selectProvince;
    private  City selectCity;
    private  int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=(TextView) view.findViewById(R.id.title_text);
        backButton=(Button) view.findViewById(R.id.back_button);
        listView=(ListView) view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return  view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE)
                {
                    selectProvince=provincesList.get(position);
                    queryCitys();
                }else if(currentLevel==LEVEL_CITY){
                    selectCity=cityList.get(position);
                    queryCuonties();

                }else if(currentLevel==LEVEL_COUNTY){
                    String weatherId=countyList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity){
                        Intent intent=new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof  WeatherActivity){
                        WeatherActivity activity=(WeatherActivity) getActivity();

                        activity.weatherId=weatherId;
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }

                  //  getActivity().finish();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCitys();
                }else  if(currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        queryProvinces();


    }

    public  void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provincesList= DataSupport.findAll(Province.class);
        if(provincesList.size()>0){
            dataList.clear();
            for(Province province:provincesList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else
        {
            String address="http://guolin.tech/api/china";
            quertFormServer(address,"province");

        }
    }

    public  void queryCitys(){
        titleText.setText(selectProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid=?",String.valueOf(selectProvince.getId())).find(City.class);
        if(cityList.size()>0)
        {
            dataList.clear();
            for(City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else{
            int procinceCode=selectProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+procinceCode;
            quertFormServer(address,"city");
        }
    }

    public  void queryCuonties(){

        titleText.setText(selectCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=DataSupport.where("cityid=?",String.valueOf(selectCity.getId())).find(County.class);
        if(countyList.size()>0)
        {
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else{
            int procinceCode=selectProvince.getProvinceCode();
            int cityCode=selectCity.getCityCode();
            String address="http://guolin.tech/api/china/"+procinceCode+"/"+cityCode;
            quertFormServer(address,"county");
        }
    }

    private  void quertFormServer(String address,final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean reslut=false;
                 if("province".equals(type)){
                     reslut= Utility.handleProvinceResponse(responseText);
                 }else if("city".equals(type)){
                     reslut=Utility.handleCityResponse(responseText,selectProvince.getId());
                 }else if("county".equals(type)){
                     reslut=Utility.handleCountyResponse(responseText,selectCity.getId());
                 }
                 if(reslut){
                     getActivity().runOnUiThread(new Runnable() {
                         @Override
                         public void run() {
                             closeProgressDialog();
                             if("province".equals(type)){
                                 queryProvinces();
                             }else if("city".equals(type)){
                                 queryCitys();
                             }else if("county".equals(type)){
                                 queryCuonties();
                             }
                         }
                     });
                 }else
                 {
                     closeProgressDialog();
                 }
            }
        });

    }

    private  void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getContext());
            progressDialog.setMessage("正在加载.....");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

    }

    private  void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
