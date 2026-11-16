package com.wirecalc.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.wirecalc.app.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isUpdating = false
    private var autoCalculatedCircleField: Int? = null // 1: R1, 2: r1, 3: R2, 4: r2
    private var autoCalculatedSquareField: Int? = null // 1: R1, 2: r1, 3: R2, 4: a

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupCircleListeners()
        setupSquareListeners()
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.layoutCircle.visibility = View.VISIBLE
                        binding.layoutSquare.visibility = View.GONE
                        binding.toolbar.subtitle = "⚪ 圆片：R₁ × r₁² = R₂ × r₂²"
                    }
                    1 -> {
                        binding.layoutCircle.visibility = View.GONE
                        binding.layoutSquare.visibility = View.VISIBLE
                        binding.toolbar.subtitle = "⬜ 方片：边长 = √(阻值1 × πr₁² / 阻值2)"
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ==================== 1. 圆片计算逻辑 ====================
    private fun setupCircleListeners() {
        val watcher: (Int) -> Unit = { fieldIndex ->
            if (!isUpdating) {
                if (autoCalculatedCircleField == fieldIndex) {
                    autoCalculatedCircleField = null
                }
                calculateCircle()
            }
        }

        binding.etR1.doAfterTextChanged { watcher(1) }
        binding.etR1Diameter.doAfterTextChanged { watcher(2) }
        binding.etR2.doAfterTextChanged { watcher(3) }
        binding.etR2Diameter.doAfterTextChanged { watcher(4) }

        binding.btnClearCircle.setOnClickListener {
            isUpdating = true
            autoCalculatedCircleField = null
            binding.etR1.setText("")
            binding.etR1Diameter.setText("")
            binding.etR2.setText("")
            binding.etR2Diameter.setText("")
            resetFieldStyle(binding.layoutR1, binding.etR1)
            resetFieldStyle(binding.layoutR1Diameter, binding.etR1Diameter)
            resetFieldStyle(binding.layoutR2, binding.etR2)
            resetFieldStyle(binding.layoutR2Diameter, binding.etR2Diameter)
            isUpdating = false
            calculateCircle()
            Toast.makeText(this, "圆片已清空", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateCircle() {
        val sR1 = binding.etR1.text?.toString()?.trim() ?: ""
        val sr1 = binding.etR1Diameter.text?.toString()?.trim() ?: ""
        val sR2 = binding.etR2.text?.toString()?.trim() ?: ""
        val sr2 = binding.etR2Diameter.text?.toString()?.trim() ?: ""

        val vR1 = sR1.toDoubleOrNull()
        val vr1 = sr1.toDoubleOrNull()
        val vR2 = sR2.toDoubleOrNull()
        val vr2 = sr2.toDoubleOrNull()

        fun isInvalid(str: String, num: Double?): Boolean = str.isNotEmpty() && (num == null || num <= 0.0)
        if (isInvalid(sR1, vR1) || isInvalid(sr1, vr1) || isInvalid(sR2, vR2) || isInvalid(sr2, vr2)) {
            binding.tvCircleResultTitle.text = "⚠️ 输入提示"
            binding.tvCircleResultValue.text = "数值必须大于 0"
            binding.tvCircleResultFormula.text = "请检查填写的数字格式。"
            return
        }

        val hasR1 = vR1 != null && vR1 > 0 && autoCalculatedCircleField != 1
        val hasr1 = vr1 != null && vr1 > 0 && autoCalculatedCircleField != 2
        val hasR2 = vR2 != null && vR2 > 0 && autoCalculatedCircleField != 3
        val hasr2 = vr2 != null && vr2 > 0 && autoCalculatedCircleField != 4

        val count = listOf(hasR1, hasr1, hasR2, hasr2).count { it }

        when (count) {
            3 -> {
                if (!hasr2) {
                    val res = sqrt((vr1!! * vr1 * vR1!!) / vR2!!)
                    val formatted = formatNumber(res)
                    isUpdating = true
                    autoCalculatedCircleField = 4
                    binding.etR2Diameter.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutR2Diameter, binding.etR2Diameter, "✨ 自动计算直径 (mm)")
                    resetFieldStyle(binding.layoutR1, binding.etR1)
                    resetFieldStyle(binding.layoutR1Diameter, binding.etR1Diameter)
                    resetFieldStyle(binding.layoutR2, binding.etR2)
                    isUpdating = false

                    binding.tvCircleResultTitle.text = "🎯 已在第4个框填入：直径 2 (r₂)"
                    binding.tvCircleResultValue.text = "$formatted mm"
                    binding.tvCircleResultFormula.text = "公式：r₂ = √(R₁ × r₁² / R₂) ≈ $formatted mm"
                } else if (!hasR2) {
                    val res = (vR1!! * vr1!! * vr1) / (vr2!! * vr2)
                    val formatted = formatNumber(res)
                    isUpdating = true
                    autoCalculatedCircleField = 3
                    binding.etR2.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutR2, binding.etR2, "✨ 自动计算阻值 (Ω)")
                    resetFieldStyle(binding.layoutR1, binding.etR1)
                    resetFieldStyle(binding.layoutR1Diameter, binding.etR1Diameter)
                    resetFieldStyle(binding.layoutR2Diameter, binding.etR2Diameter)
                    isUpdating = false

                    binding.tvCircleResultTitle.text = "🎯 已在第3个框填入：阻值 2 (R₂)"
                    binding.tvCircleResultValue.text = "$formatted Ω"
                    binding.tvCircleResultFormula.text = "公式：R₂ = (R₁ × r₁²) / r₂² ≈ $formatted Ω"
                } else if (!hasr1) {
                    val res = sqrt((vr2!! * vr2 * vR2!!) / vR1!!)
                    val formatted = formatNumber(res)
                    isUpdating = true
                    autoCalculatedCircleField = 2
                    binding.etR1Diameter.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutR1Diameter, binding.etR1Diameter, "✨ 自动计算直径 (mm)")
                    resetFieldStyle(binding.layoutR1, binding.etR1)
                    resetFieldStyle(binding.layoutR2, binding.etR2)
                    resetFieldStyle(binding.layoutR2Diameter, binding.etR2Diameter)
                    isUpdating = false

                    binding.tvCircleResultTitle.text = "🎯 已在第2个框填入：直径 1 (r₁)"
                    binding.tvCircleResultValue.text = "$formatted mm"
                    binding.tvCircleResultFormula.text = "公式：r₁ = √(R₂ × r₂² / R₁) ≈ $formatted mm"
                } else {
                    val res = (vR2!! * vr2!! * vr2) / (vr1!! * vr1)
                    val formatted = formatNumber(res)
                    isUpdating = true
                    autoCalculatedCircleField = 1
                    binding.etR1.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutR1, binding.etR1, "✨ 自动计算阻值 (Ω)")
                    resetFieldStyle(binding.layoutR1Diameter, binding.etR1Diameter)
                    resetFieldStyle(binding.layoutR2, binding.etR2)
                    resetFieldStyle(binding.layoutR2Diameter, binding.etR2Diameter)
                    isUpdating = false

                    binding.tvCircleResultTitle.text = "🎯 已在第1个框填入：阻值 1 (R₁)"
                    binding.tvCircleResultValue.text = "$formatted Ω"
                    binding.tvCircleResultFormula.text = "公式：R₁ = (R₂ × r₂²) / r₁² ≈ $formatted Ω"
                }
            }
            4 -> {
                val left = vR1!! * vr1!! * vr1
                val right = vR2!! * vr2!! * vr2
                val diff = abs(left - right)
                val pct = if (left != 0.0) (diff / left) * 100 else 0.0
                binding.tvCircleResultTitle.text = "🔍 4 项均已填写"
                binding.tvCircleResultValue.text = "相对误差: ${formatNumber(pct)}%"
                binding.tvCircleResultFormula.text = "左边(R₁·r₁²)=${formatNumber(left)}，右边(R₂·r₂²)=${formatNumber(right)}"
            }
            else -> {
                binding.tvCircleResultTitle.text = "待输入"
                binding.tvCircleResultValue.text = "已填 $count 项，还差 ${3 - count} 项"
                binding.tvCircleResultFormula.text = "输入任意 3 个数，第 4 个直接填入结果！"
                if (autoCalculatedCircleField != null) {
                    isUpdating = true
                    when (autoCalculatedCircleField) {
                        1 -> binding.etR1.setText("")
                        2 -> binding.etR1Diameter.setText("")
                        3 -> binding.etR2.setText("")
                        4 -> binding.etR2Diameter.setText("")
                    }
                    autoCalculatedCircleField = null
                    resetFieldStyle(binding.layoutR1, binding.etR1)
                    resetFieldStyle(binding.layoutR1Diameter, binding.etR1Diameter)
                    resetFieldStyle(binding.layoutR2, binding.etR2)
                    resetFieldStyle(binding.layoutR2Diameter, binding.etR2Diameter)
                    isUpdating = false
                }
            }
        }
    }

    // ==================== 2. 方片手写公式计算逻辑 ====================
    // 公式：边长 = √( 阻值1 × π × r1² / 阻值2 )
    // 即：阻值1 × (π × r1²) = 阻值2 × 边长²
    private fun setupSquareListeners() {
        val watcher: (Int) -> Unit = { fieldIndex ->
            if (!isUpdating) {
                if (autoCalculatedSquareField == fieldIndex) {
                    autoCalculatedSquareField = null
                }
                calculateSquare()
            }
        }

        binding.etSqR1.doAfterTextChanged { watcher(1) }
        binding.etSqR1Radius.doAfterTextChanged { watcher(2) }
        binding.etSqR2.doAfterTextChanged { watcher(3) }
        binding.etSqSide.doAfterTextChanged { watcher(4) }

        binding.btnClearSquare.setOnClickListener {
            isUpdating = true
            autoCalculatedSquareField = null
            binding.etSqR1.setText("")
            binding.etSqR1Radius.setText("")
            binding.etSqR2.setText("")
            binding.etSqSide.setText("")
            resetFieldStyle(binding.layoutSqR1, binding.etSqR1)
            resetFieldStyle(binding.layoutSqR1Radius, binding.etSqR1Radius)
            resetFieldStyle(binding.layoutSqR2, binding.etSqR2)
            resetFieldStyle(binding.layoutSqSide, binding.etSqSide)
            isUpdating = false
            calculateSquare()
            Toast.makeText(this, "方片已清空", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateSquare() {
        val sR1 = binding.etSqR1.text?.toString()?.trim() ?: ""
        val sr1 = binding.etSqR1Radius.text?.toString()?.trim() ?: ""
        val sR2 = binding.etSqR2.text?.toString()?.trim() ?: ""
        val sSide = binding.etSqSide.text?.toString()?.trim() ?: ""

        val vR1 = sR1.toDoubleOrNull()
        val vr1 = sr1.toDoubleOrNull()
        val vR2 = sR2.toDoubleOrNull()
        val vSide = sSide.toDoubleOrNull()

        fun isInvalid(str: String, num: Double?): Boolean = str.isNotEmpty() && (num == null || num <= 0.0)
        if (isInvalid(sR1, vR1) || isInvalid(sr1, vr1) || isInvalid(sR2, vR2) || isInvalid(sSide, vSide)) {
            binding.tvSquareResultTitle.text = "⚠️ 输入提示"
            binding.tvSquareResultValue.text = "数值必须大于 0"
            binding.tvSquareResultFormula.text = "请检查填写的数字格式。"
            return
        }

        val hasR1 = vR1 != null && vR1 > 0 && autoCalculatedSquareField != 1
        val hasr1 = vr1 != null && vr1 > 0 && autoCalculatedSquareField != 2
        val hasR2 = vR2 != null && vR2 > 0 && autoCalculatedSquareField != 3
        val hasSide = vSide != null && vSide > 0 && autoCalculatedSquareField != 4

        val count = listOf(hasR1, hasr1, hasR2, hasSide).count { it }

        when (count) {
            3 -> {
                if (!hasSide) {
                    // 1. 求 方片边长 = √(阻值1 × πr₁² / 阻值2)
                    val r1Val = vR1!!
                    val radVal = vr1!!
                    val r2Val = vR2!!
                    val inside = (r1Val * PI * radVal * radVal) / r2Val
                    val res = sqrt(inside)
                    val formatted = formatNumber(res)

                    isUpdating = true
                    autoCalculatedSquareField = 4
                    binding.etSqSide.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutSqSide, binding.etSqSide, "✨ 自动计算方片边长 (mm)")
                    resetFieldStyle(binding.layoutSqR1, binding.etSqR1)
                    resetFieldStyle(binding.layoutSqR1Radius, binding.etSqR1Radius)
                    resetFieldStyle(binding.layoutSqR2, binding.etSqR2)
                    isUpdating = false

                    binding.tvSquareResultTitle.text = "🎯 已在第4框填入：方片边长"
                    binding.tvSquareResultValue.text = "$formatted mm"
                    binding.tvSquareResultFormula.text = "手写公式：边长 = √(阻值1 × πr₁² / 阻值2)\n" +
                            "= √($r1Val × π × $radVal² / $r2Val)\n" +
                            "≈ $formatted mm"
                } else if (!hasR2) {
                    // 2. 求 阻值2 = (阻值1 × πr₁²) / 边长²
                    val r1Val = vR1!!
                    val radVal = vr1!!
                    val sideVal = vSide!!
                    val res = (r1Val * PI * radVal * radVal) / (sideVal * sideVal)
                    val formatted = formatNumber(res)

                    isUpdating = true
                    autoCalculatedSquareField = 3
                    binding.etSqR2.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutSqR2, binding.etSqR2, "✨ 自动计算阻值2 (Ω)")
                    resetFieldStyle(binding.layoutSqR1, binding.etSqR1)
                    resetFieldStyle(binding.layoutSqR1Radius, binding.etSqR1Radius)
                    resetFieldStyle(binding.layoutSqSide, binding.etSqSide)
                    isUpdating = false

                    binding.tvSquareResultTitle.text = "🎯 已在第3框填入：阻值 2 (R₂)"
                    binding.tvSquareResultValue.text = "$formatted Ω"
                    binding.tvSquareResultFormula.text = "公式：阻值2 = (阻值1 × πr₁²) / 边长²\n" +
                            "= ($r1Val × π × $radVal²) / $sideVal²\n" +
                            "≈ $formatted Ω"
                } else if (!hasr1) {
                    // 3. 求 半径1 = √( (阻值2 × 边长²) / (π × 阻值1) )
                    val r2Val = vR2!!
                    val sideVal = vSide!!
                    val r1Val = vR1!!
                    val inside = (r2Val * sideVal * sideVal) / (PI * r1Val)
                    val res = sqrt(inside)
                    val formatted = formatNumber(res)

                    isUpdating = true
                    autoCalculatedSquareField = 2
                    binding.etSqR1Radius.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutSqR1Radius, binding.etSqR1Radius, "✨ 自动计算圆半径 (mm)")
                    resetFieldStyle(binding.layoutSqR1, binding.etSqR1)
                    resetFieldStyle(binding.layoutSqR2, binding.etSqR2)
                    resetFieldStyle(binding.layoutSqSide, binding.etSqSide)
                    isUpdating = false

                    binding.tvSquareResultTitle.text = "🎯 已在第2框填入：圆半径 1 (r₁)"
                    binding.tvSquareResultValue.text = "$formatted mm"
                    binding.tvSquareResultFormula.text = "公式：r₁ = √(阻值2 × 边长² / (π × 阻值1))\n≈ $formatted mm"
                } else {
                    // 4. 求 阻值1 = (阻值2 × 边长²) / (π × r1²)
                    val r2Val = vR2!!
                    val sideVal = vSide!!
                    val radVal = vr1!!
                    val res = (r2Val * sideVal * sideVal) / (PI * radVal * radVal)
                    val formatted = formatNumber(res)

                    isUpdating = true
                    autoCalculatedSquareField = 1
                    binding.etSqR1.setText(formatted)
                    setCalculatedFieldStyle(binding.layoutSqR1, binding.etSqR1, "✨ 自动计算阻值1 (Ω)")
                    resetFieldStyle(binding.layoutSqR1Radius, binding.etSqR1Radius)
                    resetFieldStyle(binding.layoutSqR2, binding.etSqR2)
                    resetFieldStyle(binding.layoutSqSide, binding.etSqSide)
                    isUpdating = false

                    binding.tvSquareResultTitle.text = "🎯 已在第1框填入：阻值 1 (R₁)"
                    binding.tvSquareResultValue.text = "$formatted Ω"
                    binding.tvSquareResultFormula.text = "公式：阻值1 = (阻值2 × 边长²) / (π × r₁²)\n≈ $formatted Ω"
                }
            }
            4 -> {
                val circleArea = PI * vr1!! * vr1
                val squareArea = vSide!! * vSide
                val left = vR1!! * circleArea
                val right = vR2!! * squareArea
                val diff = abs(left - right)
                val pct = if (left != 0.0) (diff / left) * 100 else 0.0
                binding.tvSquareResultTitle.text = "🔍 4 项均已填写"
                binding.tvSquareResultValue.text = "相对误差: ${formatNumber(pct)}%"
                binding.tvSquareResultFormula.text = "等式两端对比：\n阻值1·πr₁² = ${formatNumber(left)}\n阻值2·边长² = ${formatNumber(right)}"
            }
            else -> {
                binding.tvSquareResultTitle.text = "待输入"
                binding.tvSquareResultValue.text = "已填 $count 项，还差 ${3 - count} 项"
                binding.tvSquareResultFormula.text = "手写公式：边长 = √(阻值1 × πr₁² / 阻值2)\n输入任意 3 项即可！"
                if (autoCalculatedSquareField != null) {
                    isUpdating = true
                    when (autoCalculatedSquareField) {
                        1 -> binding.etSqR1.setText("")
                        2 -> binding.etSqR1Radius.setText("")
                        3 -> binding.etSqR2.setText("")
                        4 -> binding.etSqSide.setText("")
                    }
                    autoCalculatedSquareField = null
                    resetFieldStyle(binding.layoutSqR1, binding.etSqR1)
                    resetFieldStyle(binding.layoutSqR1Radius, binding.etSqR1Radius)
                    resetFieldStyle(binding.layoutSqR2, binding.etSqR2)
                    resetFieldStyle(binding.layoutSqSide, binding.etSqSide)
                    isUpdating = false
                }
            }
        }
    }

    private fun setCalculatedFieldStyle(layout: TextInputLayout, editText: EditText, helperText: String) {
        layout.helperText = helperText
        layout.setHelperTextColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")))
        editText.setTextColor(Color.parseColor("#1B5E20"))
    }

    private fun resetFieldStyle(layout: TextInputLayout, editText: EditText) {
        layout.helperText = null
        editText.setTextColor(Color.parseColor("#212121"))
    }

    private fun formatNumber(value: Double): String {
        return if (value.isNaN() || value.isInfinite()) {
            "NaN"
        } else {
            BigDecimal(value)
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        }
    }
}
