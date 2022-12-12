package com.lgt.cwm.activity.home.fragments.contact.models

class SelectedContact constructor(id: String?, name: String?, number: String?) {
    val id: String?
    val name: String?
    val number: String?

    init {
        this.id = id
        this.name = name
        this.number = number
    }

    fun matches(other: SelectedContact?): Boolean {
        if (other == null) return false
        return if (id != null && other.id != null) {
            id == other.id
        } else (number != null && number == other.number) ||
                (name != null && name == other.name)
    }

}