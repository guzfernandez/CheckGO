package com.guzf.checkgo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Guz on 8/10/16.
 */
public class CustomAdapter extends BaseAdapter {

    Context context;
    List<User> users;

    public CustomAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public User getItem(int i) {
        return users.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    static class ViewHolder {
        private TextView tvNumber, tvInfo;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final ViewHolder holder;

        final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (view == null) {
            view = inflater.inflate(R.layout.ranking_layout, null);
            holder = new ViewHolder();

            holder.tvNumber = (TextView) view.findViewById(R.id.tvNumber);
            holder.tvInfo = (TextView) view.findViewById(R.id.tvInfo);

            view.setTag(holder);

        } else {
            holder = (ViewHolder) view.getTag();
        }

        User user = getItem(i);

        holder.tvNumber.setText("#"+(i+1));

        if(user.getPuntaje() == 1){
            holder.tvInfo.setText(user.getNombre() + "  |  " + user.getPuntaje() + " punto");
        }
        else{
            holder.tvInfo.setText(user.getNombre() + "  |  " + user.getPuntaje() + " puntos");
        }

        return view;
    }
}
