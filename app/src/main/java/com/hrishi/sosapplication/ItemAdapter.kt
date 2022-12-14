package com.hrishi.sosapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hrishi.sosapplication.databinding.ItemsBinding

class ItemAdapter(private var items:ArrayList<ContactEntity>):RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

    class ViewHolder(binding: ItemsBinding):RecyclerView.ViewHolder(binding.root){
        val tvName=binding.tvName
        val tvPhone=binding.tvPhone
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemsBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text=items[position].name
        holder.tvPhone.text=items[position].phone
    }

    override fun getItemCount(): Int {
        return items.size
    }
}