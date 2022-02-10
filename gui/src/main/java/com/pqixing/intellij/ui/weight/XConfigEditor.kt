package com.pqixing.intellij.ui.weight

import com.pqixing.model.define.IConfig
import java.lang.reflect.Field

class XConfigEditor {
    companion object {

        private fun eachConfigField(block: (field: Field) -> Unit) {
            for (it in IConfig::class.java.declaredFields) kotlin.runCatching {
                it.isAccessible = true
                block(it)
            }
        }

        fun parseConfigChange(target: IConfig): Map<String, Any> {
            //默认的配置信息,对比当前config和默认config,如果值不同,同时不为空,则写入配置文件
            val defConfig = IConfig()
            val result = mutableMapOf<String, Any>()

            //遍历当前所有函数的对象
            eachConfigField { field ->
                val defValue = field.get(defConfig)
                val tarValue = field.get(target)

                if (defValue is Boolean && tarValue is Boolean && defValue != tarValue) {
                    result[field.name] = tarValue
                } else if (defValue is String && tarValue is String && defValue != tarValue && tarValue.isNotEmpty()) {
                    result[field.name] = tarValue
                }
            }
            return result
        }
    }
}