package cl.duoc.museo.data.local

import androidx.room.TypeConverter
import cl.duoc.museo.data.model.Role

class RoleConverter {
    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(roleStr: String): Role {
        return try {
            Role.valueOf(roleStr)
        } catch (e: Exception) {
            Role.CLIENT
        }
    }
}