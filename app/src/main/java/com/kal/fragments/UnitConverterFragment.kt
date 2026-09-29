package com.kal.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.kal.R
import com.kal.calculator.Evaluator
import com.kal.converter.UnitActionListener
import com.kal.converter.UnitAdapter
import com.kal.converter.data.AngleConverter
import com.kal.converter.data.AreaConverter
import com.kal.converter.data.DensityConverter
import com.kal.converter.data.DigitalStorageConverter
import com.kal.converter.data.EnergyConverter
import com.kal.converter.data.ForceConverter
import com.kal.converter.data.FrequencyConverter
import com.kal.converter.data.FuelConverter
import com.kal.converter.data.LengthConverter
import com.kal.converter.data.LightConverter
import com.kal.converter.data.MassConverter
import com.kal.converter.data.PowerConverter
import com.kal.converter.data.PressureConverter
import com.kal.converter.data.SpeedConverter
import com.kal.converter.data.TemperatureConverter
import com.kal.converter.data.TimeConverter
import com.kal.converter.data.TorqueConverter
import com.kal.converter.data.UnitConverter
import com.kal.converter.data.ViscosityConverter
import com.kal.converter.data.VolumeConverter
import com.kal.databinding.FragmentUnitConverterBinding
import com.kal.utils.InteractionAndroid
import kotlin.properties.Delegates.notNull

class UnitConverterFragment : Fragment() {

    companion object {
        var physicalQuantity = 10
        var unit = 1010
    }

    private val lengthConverter = LengthConverter()
    private val timeConverter = TimeConverter()
    private val massConverter = MassConverter()
    private val temperatureConverter = TemperatureConverter()
    private val speedConverter = SpeedConverter()
    private val forceConverter = ForceConverter()
    private val energyConverter = EnergyConverter()
    private val powerConverter = PowerConverter()
    private val pressureConverter = PressureConverter()
    private val volumeConverter = VolumeConverter()
    private val areaConverter = AreaConverter()
    private val densityConverter = DensityConverter()
    private val frequencyConverter = FrequencyConverter()
    private val torqueConverter = TorqueConverter()
    private val lightConverter = LightConverter()
    private val digitalStorageConverter = DigitalStorageConverter()
    private val viscosityConverter = ViscosityConverter()
    private val angleConverter = AngleConverter()
    private val fuelConverter = FuelConverter()

    private lateinit var physicalQuantities: List<Pair<String, UnitConverter>>
    private var binding: FragmentUnitConverterBinding by notNull()
    private var adapter: UnitAdapter by notNull()
    private var callback: OnButtonClickListener? = null

    interface OnButtonClickListener {
        fun onUnitResultTextClick(result: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            callback = parentFragment as OnButtonClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnButtonClickListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUnitConverterBinding.inflate(inflater, container, false)

        adapter = UnitAdapter(requireContext(), object : UnitActionListener {
            override fun onUnitCopyButtonClick(result: String) {
                InteractionAndroid.copyToClipboard(result, requireContext())
            }

            override fun onUnitResultTextClick(result: String) {
                callback?.onUnitResultTextClick(result)
            }

            override fun onUnitResultTextLongClick(result: String) {
                InteractionAndroid.copyToClipboard(result, requireContext())
            }
        })

        val layoutManager = LinearLayoutManager(requireContext())
        binding.unitRecyclerView.layoutManager = layoutManager
        binding.unitRecyclerView.adapter = adapter

        physicalQuantities = listOf(
            Pair(getString(R.string.physical_quantity_length), lengthConverter),
            Pair(getString(R.string.physical_quantity_time), timeConverter),
            Pair(getString(R.string.physical_quantity_mass), massConverter),
            Pair(getString(R.string.physical_quantity_temperature), temperatureConverter),
            Pair(getString(R.string.physical_quantity_speed), speedConverter),
            Pair(getString(R.string.physical_quantity_force), forceConverter),
            Pair(getString(R.string.physical_quantity_energy), energyConverter),
            Pair(getString(R.string.physical_quantity_power), powerConverter),
            Pair(getString(R.string.physical_quantity_pressure), pressureConverter),
            Pair(getString(R.string.physical_quantity_volume), volumeConverter),
            Pair(getString(R.string.physical_quantity_area), areaConverter),
            Pair(getString(R.string.physical_quantity_density), densityConverter),
            Pair(getString(R.string.physical_quantity_frequency), frequencyConverter),
            Pair(getString(R.string.physical_quantity_torque), torqueConverter),
            Pair(getString(R.string.physical_quantity_light), lightConverter),
            Pair(getString(R.string.physical_quantity_storage), digitalStorageConverter),
            Pair(getString(R.string.physical_quantity_viscosity), viscosityConverter),
            Pair(getString(R.string.physical_quantity_angle), angleConverter),
            Pair(getString(R.string.physical_quantity_fuel), fuelConverter)
        )
            .sortedBy { it.second.id }


        val physicalQuantityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, physicalQuantities.map { it.first })
        binding.physicalQuantityAutoCompleteTextView.setAdapter(physicalQuantityAdapter)


        val initialQuantity = physicalQuantities.find { it.second.id == physicalQuantity }
        if (initialQuantity != null) {
            binding.physicalQuantityAutoCompleteTextView.setText(initialQuantity.first, false)
            binding.physicalQuantityAutoCompleteTextView.contentDescription = binding.physicalQuantityAutoCompleteTextView.text
            updateUnitAutoComplete(initialQuantity.second)
        }


        binding.physicalQuantityAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            binding.physicalQuantityAutoCompleteTextView.contentDescription = binding.physicalQuantityAutoCompleteTextView.text
            val selectedQuantity = physicalQuantities[position]
            physicalQuantity = selectedQuantity.second.id
            updateUnitAutoComplete(selectedQuantity.second)
        }

        binding.unitAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            binding.unitAutoCompleteTextView.contentDescription = binding.unitAutoCompleteTextView.text
            val selectedQuantity = physicalQuantities.find { it.second.id == physicalQuantity } ?: return@setOnItemClickListener
            val selectedUnit = selectedQuantity.second.units.sortedBy { it.id }.getOrNull(position)
                ?: return@setOnItemClickListener
            unit = selectedUnit.id
            updateUnitPager(Evaluator.converterResult.value, selectedQuantity.second, unit)
        }


        Evaluator.converterResult.observe(requireActivity()){ converterResult ->
            val selectedQuantity = physicalQuantities.find { it.second.id == physicalQuantity }
                ?: return@observe
            updateUnitPager(converterResult, selectedQuantity.second, unit)
        }

        return binding.root
    }

    private fun updateUnitAutoComplete(unitConverter: UnitConverter) {
        val units = unitConverter.units.sortedBy { it.id }
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units.map { getString(it.name) })
        binding.unitAutoCompleteTextView.setAdapter(unitAdapter)

        if (units.isNotEmpty()) {
            val selectedUnit = units.find { it.id == unit }
            if (selectedUnit != null) {
                unit = selectedUnit.id
                binding.unitAutoCompleteTextView.setText(getString(selectedUnit.name), false)
                binding.unitAutoCompleteTextView.contentDescription = binding.unitAutoCompleteTextView.text
            } else {
                unit =  units[0].id
                binding.unitAutoCompleteTextView.setText(getString(units[0].name), false)
            }

            updateUnitPager(Evaluator.converterResult.value, unitConverter, unit)
        }
    }

    private fun updateUnitPager(value: Double?, unitConverter: UnitConverter, unitIndex: Int){
        if (value == null){
            adapter.clearUnits()
            return
        }

        val inputUnit = unitConverter.units.sortedBy { it.id }.find { it.id == unitIndex }
        val results = inputUnit?.let { unitConverter.convertAll(value, it) }

        adapter.updateUnits(results, inputUnit)
    }
}
