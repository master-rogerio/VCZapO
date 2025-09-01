package com.pdm.vczap_o.contacts.data

import android.content.Context
import android.provider.ContactsContract
import com.pdm.vczap_o.contacts.domain.model.Contact
import javax.inject.Inject

class ContactsRepository @Inject constructor(
    private val context: Context
) {

    fun getContacts(): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameIndex != -1) it.getString(nameIndex) else ""
                val number = if (numberIndex != -1) it.getString(numberIndex) else ""
                if (name.isNotBlank() && number.isNotBlank()) {
                    contactsList.add(Contact(name = name, phoneNumber = number))
                }
            }
        }
        return contactsList.distinctBy { it.phoneNumber } // Remove duplicados
    }
}