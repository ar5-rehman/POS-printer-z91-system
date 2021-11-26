package com.pos.food.Adapters;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.amazonaws.amplify.generated.graphql.ListOrdersQuery;
import com.google.gson.Gson;
import com.pos.food.MainActivity;
import com.pos.food.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class NewOrdersAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<ListOrdersQuery.Item> orderslist;

    Context mContext;

    public NewOrdersAdapter(Context context, List<ListOrdersQuery.Item> orderslist){
        this.mContext = context;

        this.orderslist = orderslist;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setOrders(List<ListOrdersQuery.Item> posts){
        this.orderslist = posts;
    }

    @Override
    public int getCount() {
        return orderslist.size();
    }

    @Override
    public Object getItem(int i) {
        return orderslist.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.adapter_new_orders_layout_item, parent, false);
            holder = new ViewHolder();




            holder.txtcustomername = convertView.findViewById(R.id.txtcustomername);
            holder.txtprice = convertView.findViewById(R.id.txtprice);
            holder.txttime=convertView.findViewById(R.id.txttime);
            holder.vieworderlayout=convertView.findViewById(R.id.vieworderlayout);

            holder.acceptorderlayout = convertView.findViewById(R.id.acceptorderlayout);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }




        final ListOrdersQuery.Item order = (ListOrdersQuery.Item) getItem(i);

       /* if(order.status()!=null)
        {
            Log.e("orderstatus", orderslist.get(i).foodItems());


        }*/

        holder.txtcustomername.setText("Kunde: " + order.customerName());
        holder.txtprice.setText("Pris: " + order.total());
        holder.txttime.setText("Tid: " + order.derliveryTime());

        holder.vieworderlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("hello","howareyou");
                ((MainActivity) mContext).printMatrixText(0, order.restaurantName(), order.restaurantNote(), order.createdAt(), order.foodItems(), ""+order.total(), order.customerName(), order.customerID(), order.address());
            }
        });

        holder.acceptorderlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("hello","howareyou");

                ((MainActivity) mContext).printMatrixText(0, order.restaurantName(), order.restaurantNote(), order.createdAt(), order.foodItems(), ""+order.total(), order.customerName(), order.customerID(), order.address());

            }
        });





        return convertView;
    }

    private static class ViewHolder {


        TextView txtcustomername,txtprice,txttime;

        CardView vieworderlayout,acceptorderlayout;

    }
}



