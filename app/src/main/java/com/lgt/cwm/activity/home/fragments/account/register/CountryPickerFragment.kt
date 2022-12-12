package com.lgt.cwm.activity.home.fragments.account.register

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.lgt.cwm.R
import com.lgt.cwm.activity.home.fragments.account.adapter.CountryCodeListAdapter
import com.lgt.cwm.activity.home.fragments.account.models.Country
import com.lgt.cwm.databinding.FragmentCountryPickerBinding
import com.lgt.cwm.util.DebugConfig
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject


@AndroidEntryPoint
class CountryPickerFragment : Fragment() {
    private val TAG = CountryPickerFragment::class.simpleName.toString()

    companion object {
        const val REQUEST_COUNTRY_SELECT = "country_select"
        const val KEY_COUNTRY = "country"
        const val KEY_COUNTRY_CODE = "country_code"
    }

    @Inject
    lateinit var debugConfig: DebugConfig

    private val countries: MutableList<Country> = mutableListOf()

    @Inject lateinit var countryCodeListAdapter: CountryCodeListAdapter

    private val countryPickerViewModel: CountryPickerViewModel by viewModels()

    private lateinit var countrySearch: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: FragmentCountryPickerBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_country_picker, container, false);
        binding.lifecycleOwner = viewLifecycleOwner
        binding.countryPickerViewModel = countryPickerViewModel
        binding.countryCodeListAdapter = countryCodeListAdapter
        countrySearch = binding.countrySearch

        binding.countrySearch.addTextChangedListener(FilterWatcher())

        val dividerItemDecoration = DividerItemDecoration(binding.listCountry.context, LinearLayout.VERTICAL)
        binding.listCountry.addItemDecoration(dividerItemDecoration)


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        debugConfig.log(TAG, "onViewCreated")

        initCountryList()
        countryCodeListAdapter.setItems(countries)
        applyFilter(countrySearch.text)

        countryCodeListAdapter.setOnItemClickListener(object :
            CountryCodeListAdapter.OnItemClickListener {
            override fun onItemActiveClick(item: Country, position: Int) {
//                debugConfig.log(TAG, "onItemActiveClick ${item.code} - ${item.name}")

                val result = Bundle()
                result.putString(KEY_COUNTRY, item.name)
                result.putString(KEY_COUNTRY_CODE, item.code)
                parentFragmentManager.setFragmentResult(REQUEST_COUNTRY_SELECT, result)
                findNavController().navigateUp();
            }
        })
    }

    private fun initCountryList() {
        try {
            val stream = InputStreamReader(resources.assets.open("countries.txt"))
            val reader = BufferedReader(stream)
            var line: String
            while (reader.readLine().also { line = it } != null) {
                val args = line.split(";").toTypedArray()
                val country = Country()
                country.code = args[0]
                country.shortname = args[1]
                country.name = args[2]

                countries.add(country)
            }
            reader.close()
            stream.close()
        } catch (e: Exception) {

        }

        countries.sortBy { it.name }
    }

    private fun applyFilter(text: CharSequence) {
        countryCodeListAdapter.filter.filter(text)
    }

    private inner class FilterWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            applyFilter(s)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

}