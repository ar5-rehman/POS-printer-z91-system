package com.pos.food;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;


import com.amazonaws.amplify.generated.graphql.ListOrdersQuery;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.appsync.AWSAppSyncClient;
import com.amazonaws.mobileconnectors.appsync.fetcher.AppSyncResponseFetchers;
import com.apollographql.apollo.GraphQLCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.pos.food.Adapters.AcceptedOrdersAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class AcceptedOrdersActivity extends AppCompatActivity {



    AcceptedOrdersAdapter adapter;
    ListView orderslistview;

    private AWSAppSyncClient mAWSAppSyncClient;
    private List<ListOrdersQuery.Item> orderslist = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accepted_orders);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent=getIntent();



        getSupportActionBar().setTitle(intent.getStringExtra("screentitle"));
        orderslistview = findViewById(R.id.orderslistview);
        mAWSAppSyncClient = AWSAppSyncClient.builder()
                .context(getApplicationContext())
                .awsConfiguration(new AWSConfiguration(getApplicationContext()))
                .build();

        adapter = new AcceptedOrdersAdapter(AcceptedOrdersActivity.this, orderslist);
        orderslistview.setAdapter(adapter);

        queryOrders();
        
    }


    private void queryOrders() {

        mAWSAppSyncClient.query(ListOrdersQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(todosCallback);


       /* mAWSAppSyncClient.query(ListRestaurantsQuery.builder().build())
                .responseFetcher(AppSyncResponseFetchers.CACHE_AND_NETWORK)
                .enqueue(todosCallback1);*/
    }

    private GraphQLCall.Callback<ListOrdersQuery.Data> todosCallback = new GraphQLCall.Callback<ListOrdersQuery.Data>() {
        @Override
        public void onResponse(@Nonnull Response<ListOrdersQuery.Data> response) {
            orderslist = response.data().listOrders().items();
            if (response.data() != null) {
                orderslist = response.data().listOrders().items();
            } else {
                orderslist = new ArrayList<>();
            }
            adapter.setOrders(orderslist);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("changednotify", "Notifying data set changed");
                    adapter.notifyDataSetChanged();

                }
            });

            /*int i;
            for (i = 0; i < orderslist.size(); i++) {
                Log.e("customername", orderslist.get(i).id());
            }


            adapter.notifyDataSetChanged();
            Log.e("orderslistsize", orderslist.size() + "");*/


        }

        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e("ERRORofresponse", e.toString());
        }
    };
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }


}