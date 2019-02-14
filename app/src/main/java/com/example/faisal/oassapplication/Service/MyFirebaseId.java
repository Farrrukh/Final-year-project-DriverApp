package com.example.faisal.oassapplication.Service;

import com.example.faisal.oassapplication.Common.Common;
import com.example.faisal.oassapplication.Model.Token;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class MyFirebaseId extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String refreshedToken =FirebaseInstanceId.getInstance().getToken();
        updateTokenServer(refreshedToken);

    }

    private void updateTokenServer(String refreshedToken) {
        FirebaseDatabase db=FirebaseDatabase.getInstance();
        DatabaseReference tokens=db.getReference(Common.token_tbl);
        Token token=new Token(refreshedToken);
        if(FirebaseAuth.getInstance().getCurrentUser()!=null)
            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
    }
}
