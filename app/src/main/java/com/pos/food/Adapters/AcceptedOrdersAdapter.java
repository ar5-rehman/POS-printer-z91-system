package com.pos.food.Adapters;


import com.amazonaws.amplify.generated.graphql.ListOrdersQuery;
import com.pos.food.MainActivity;
import com.pos.food.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.util.List;


public class AcceptedOrdersAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<ListOrdersQuery.Item> orderslist;

    Context mContext;

    public AcceptedOrdersAdapter(Context context, List<ListOrdersQuery.Item> orderslist) {
        this.mContext = context;
        this.orderslist = orderslist;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setOrders(List<ListOrdersQuery.Item> posts) {
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
            convertView = mInflater.inflate(R.layout.adapter_accept_orders_layout_item, parent, false);
            holder = new ViewHolder();


            holder.txtcustomername = convertView.findViewById(R.id.txtcustomername);
            holder.txtprice = convertView.findViewById(R.id.txtprice);
            holder.txttime = convertView.findViewById(R.id.txttime);
            holder.vieworderlayout = convertView.findViewById(R.id.vieworderlayout);
            holder.acceptorderlayout = convertView.findViewById(R.id.acceptorderlayout);


            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }


        final ListOrdersQuery.Item order = (ListOrdersQuery.Item) getItem(i);

        if (order.status() != null) {
            Log.e("orderstatus", orderslist.get(i).status());

        }

        holder.txtcustomername.setText("Kunde: " + order.customerName());
        holder.txtprice.setText("Pris: " + order.total());
        holder.txttime.setText("Tid: " + order.derliveryTime());

       /* holder.vieworderlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("hello","howareyou");
                ((MainActivity) mContext).printMatrixText(0);
            }
        });

        holder.acceptorderlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("hello","howareyou");
                ((MainActivity) mContext).printMatrixText(0);

            }
        });

*/
        return convertView;
    }

    private static class ViewHolder {


        TextView txtcustomername, txtprice, txttime;

        CardView vieworderlayout, acceptorderlayout;
    }
}



