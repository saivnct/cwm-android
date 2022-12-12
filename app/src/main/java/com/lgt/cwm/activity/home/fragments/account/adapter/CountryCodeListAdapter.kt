package com.lgt.cwm.activity.home.fragments.account.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.lgt.cwm.R
import com.lgt.cwm.activity.home.fragments.account.models.Country
import com.lgt.cwm.databinding.CountryCodeItemBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.scopes.FragmentScoped
import java.util.*
import javax.inject.Inject


@FragmentScoped
class CountryCodeListAdapter @Inject constructor(): Filterable,  RecyclerView.Adapter<CountryCodeListAdapter.CountryCodeListViewHolder>() {

    @Inject
    lateinit var debugConfig: DebugConfig

    private lateinit var context: Context

    private var items: List<Country> = arrayListOf()
    private var itemsFilter: List<Country> = arrayListOf()

    private val itemSearchMap = mutableMapOf<Country, List<String>>()

    private var listener: OnItemClickListener? = null

    fun setItems(items: List<Country>){
        this.items = items;

        for (item in items) {
            itemSearchMap[item] = item.name.split(" ").toList()
        }

        notifyDataSetChanged();
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    class CountryCodeListViewHolder(val binding: CountryCodeItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind() {
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): CountryCodeListViewHolder {
        context = viewGroup.context
        val binding: CountryCodeItemBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.country_code_item, viewGroup, false)
        return CountryCodeListViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return itemsFilter.size
    }

    override fun onBindViewHolder(holder: CountryCodeListViewHolder, position: Int) {
        val item = itemsFilter[position]
        val binding = holder.binding

        binding.title.text = item.name
        binding.subTitle.text = "+" + item.code

        binding.viewCountryCodeItem.setOnClickListener {
            listener?.onItemActiveClick(item, position)
        }

        holder.bind();
    }

    interface OnItemClickListener {
        fun onItemActiveClick(item: Country, position: Int)
    }

    //region filter
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults) {
                itemsFilter = filterResults.values as List<Country>
                notifyDataSetChanged()
            }

            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val q = charSequence.toString().trim { it <= ' ' }.lowercase(Locale.getDefault())
                if (q.isEmpty()) {
                    itemsFilter = items
                }
                val results = mutableListOf<Country>()
                for (country in items) {
                    itemSearchMap[country]?.let {
                        for (key in it) {
                            if (key.lowercase(Locale.getDefault()).startsWith(q)) {
                                results.add(country)
                                break
                            }
                        }
                    }

                }

                val filterResults = FilterResults()
                filterResults.values = results
                return filterResults
            }
        }
    }
    //endregion filter
}