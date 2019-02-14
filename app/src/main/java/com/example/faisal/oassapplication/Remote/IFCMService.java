package com.example.faisal.oassapplication.Remote;

import com.example.faisal.oassapplication.Model.FCMResponse;
import com.example.faisal.oassapplication.Model.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAGf3-S9E:APA91bGmhNXdYQUt7grZyUact3iwIjS5_tnVCFPY7yHbE8T0g_m1191A2PcDqUC0LS3hMXaGYWKmlSBQ1YCsVcxb5DY3cVjbalkKRMb980gMn7KS1HiLFE_x6NMvNqnkU35MyC5zu94q"
    })
    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body Sender body);
}
