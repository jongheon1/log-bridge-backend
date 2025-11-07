package io.channel.vocinsight.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<String>?): String {
        return if (attribute.isNullOrEmpty()) {
            "[]"
        } else {
            objectMapper.writeValueAsString(attribute)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): List<String> {
        return if (dbData.isNullOrBlank() || dbData == "[]") {
            emptyList()
        } else {
            try {
                objectMapper.readValue(dbData, object : TypeReference<List<String>>() {})
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
