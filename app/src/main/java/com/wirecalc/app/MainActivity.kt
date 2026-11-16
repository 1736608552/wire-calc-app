package com.wirecalc.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.wirecalc.app.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var lastResultText: String = ""
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        calculate()
    }

    private fun setupListeners() {
        val watcher: (CharSequence?) -> Unit = {
            if (!isUpdating) {
                calculate()
            }
        }

        binding.etR1.doAfterTextChanged { watcher(it) }
        binding.etR1Diameter.doAfterTextChanged { watcher(it) }
        binding.etR2.doAfterTextChanged { watcher(it) }
        binding.etR2Diameter.doAfterTextChanged { watcher(it) }

        binding.btnClear.setOnClickListener {
            isUpdating = true
            binding.etR1.setText("")
            binding.etR1Diameter.setText("")
            binding.etR2.setText("")
            binding.etR2Diameter.setText("")
            isUpdating = false
            calculate()
            Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
        }

        binding.btnCopyResult.setOnClickListener {
            if (lastResultText.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("WireCalc Result", lastResultText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "结果已复制到剪贴板: $lastResultText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculate() {
        val sR1 = binding.etR1.text?.toString()?.trim() ?: ""
        val sr1 = binding.etR1Diameter.text?.toString()?.trim() ?: ""
        val sR2 = binding.etR2.text?.toString()?.trim() ?: ""
        val sr2 = binding.etR2Diameter.text?.toString()?.trim() ?: ""

        val vR1 = sR1.toDoubleOrNull()
        val vr1 = sr1.toDoubleOrNull()
        val vR2 = sR2.toDoubleOrNull()
        val vr2 = sr2.toDoubleOrNull()

        // 验证输入的合法性（若输入非空但无法解析或 <=0）
        fun isInvalid(str: String, num: Double?): Boolean = str.isNotEmpty() && (num == null || num <= 0.0)

        if (isInvalid(sR1, vR1) || isInvalid(sr1, vr1) || isInvalid(sR2, vR2) || isInvalid(sr2, vr2)) {
            binding.tvResultTitle.text = "⚠️ 输入提示"
            binding.tvResultValue.text = "数值必须大于 0"
            binding.tvResultFormula.text = "请检查填写的数字格式是否正确且为正数。"
            binding.btnCopyResult.visibility = View.GONE
            return
        }

        val hasR1 = vR1 != null && vR1 > 0
        val hasr1 = vr1 != null && vr1 > 0
        val hasR2 = vR2 != null && vR2 > 0
        val hasr2 = vr2 != null && vr2 > 0

        val filledCount = listOf(hasR1, hasr1, hasR2, hasr2).count { it }

        when (filledCount) {
            3 -> {
                // 恰好 3 个，计算第 4 个
                binding.btnCopyResult.visibility = View.VISIBLE
                if (!hasr2) {
                    // 求 直径2 (r2) - 即用户截图红圈公式
                    val r1Val = vR1!!
                    val d1Val = vr1!!
                    val r2Val = vR2!!
                    val inside = (d1Val * d1Val * r1Val) / r2Val
                    val result = sqrt(inside)
                    val formatted = formatNumber(result)
                    lastResultText = formatted

                    binding.tvResultTitle.text = "🎯 计算目标：直径 2 (r₂)"
                    binding.tvResultValue.text = "$formatted mm"
                    binding.tvResultFormula.text = "公式：r₂ = √(R₁ × r₁² / R₂)\n" +
                            "= √($r1Val × $d1Val² / $r2Val)\n" +
                            "= √(${formatNumber(inside)})\n" +
                            "≈ $formatted mm"
                } else if (!hasR2) {
                    // 求 阻值2 (R2)
                    val r1Val = vR1!!
                    val d1Val = vr1!!
                    val d2Val = vr2!!
                    val result = (r1Val * d1Val * d1Val) / (d2Val * d2Val)
                    val formatted = formatNumber(result)
                    lastResultText = formatted

                    binding.tvResultTitle.text = "🎯 计算目标：阻值 2 (R₂)"
                    binding.tvResultValue.text = "$formatted Ω"
                    binding.tvResultFormula.text = "公式：R₂ = (R₁ × r₁²) / r₂²\n" +
                            "= ($r1Val × $d1Val²) / $d2Val²\n" +
                            "= ${formatNumber(r1Val * d1Val * d1Val)} / ${formatNumber(d2Val * d2Val)}\n" +
                            "≈ $formatted Ω"
                } else if (!hasr1) {
                    // 求 直径1 (r1)
                    val r2Val = vR2!!
                    val d2Val = vr2!!
                    val r1Val = vR1!!
                    val inside = (d2Val * d2Val * r2Val) / r1Val
                    val result = sqrt(inside)
                    val formatted = formatNumber(result)
                    lastResultText = formatted

                    binding.tvResultTitle.text = "🎯 计算目标：直径 1 (r₁)"
                    binding.tvResultValue.text = "$formatted mm"
                    binding.tvResultFormula.text = "公式：r₁ = √(R₂ × r₂² / R₁)\n" +
                            "= √($r2Val × $d2Val² / $r1Val)\n" +
                            "= √(${formatNumber(inside)})\n" +
                            "≈ $formatted mm"
                } else {
                    // 求 阻值1 (R1)
                    val r2Val = vR2!!
                    val d2Val = vr2!!
                    val d1Val = vr1!!
                    val result = (r2Val * d2Val * d2Val) / (d1Val * d1Val)
                    val formatted = formatNumber(result)
                    lastResultText = formatted

                    binding.tvResultTitle.text = "🎯 计算目标：阻值 1 (R₁)"
                    binding.tvResultValue.text = "$formatted Ω"
                    binding.tvResultFormula.text = "公式：R₁ = (R₂ × r₂²) / r₁²\n" +
                            "= ($r2Val × $d2Val²) / $d1Val²\n" +
                            "= ${formatNumber(r2Val * d2Val * d2Val)} / ${formatNumber(d1Val * d1Val)}\n" +
                            "≈ $formatted Ω"
                }
            }
            4 -> {
                binding.btnCopyResult.visibility = View.GONE
                val left = vR1!! * vr1!! * vr1
                val right = vR2!! * vr2!! * vr2
                val diff = abs(left - right)
                val percent = if (left != 0.0) (diff / left) * 100 else 0.0

                binding.tvResultTitle.text = "🔍 4 项均已填写"
                binding.tvResultValue.text = "误差: ${formatNumber(percent)}%"
                binding.tvResultFormula.text = "等式检验：\n" +
                        "左边 (R₁·r₁²) = ${formatNumber(left)}\n" +
                        "右边 (R₂·r₂²) = ${formatNumber(right)}\n" +
                        "提示：若想计算某项，可直接清空该项的输入框。"
            }
            else -> {
                binding.btnCopyResult.visibility = View.GONE
                binding.tvResultTitle.text = "待计算"
                binding.tvResultValue.text = "已填 $filledCount 项，还差 ${3 - filledCount} 项"
                binding.tvResultFormula.text = "提示：输入任意 3 个数，留空的那个数就会自动算出！"
            }
        }
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
