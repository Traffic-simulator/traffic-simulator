package ru.nsu.trafficsimulator.graphics

import com.badlogic.gdx.graphics.g3d.Attribute

class RoadMaterialAttribute : Attribute(getType()) {
    override fun compareTo(other: Attribute?): Int {
        if (other == null) {
            return 1
        }
        return if (other is RoadMaterialAttribute) { 0 } else { 1 }
    }

    override fun copy(): Attribute {
        return RoadMaterialAttribute()
    }

    companion object {
        const val alias = "RoadAttribute"

        fun getType(): Long = register(alias)
    }
}
