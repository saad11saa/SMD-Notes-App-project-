package com.example.datastorageinandroidapp

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.datastorageinandroidapp.ui.theme.DataStorageInAndroidAppTheme
import com.example.datastorageinandroidapp.navigateToAppSettings as navigateToAppSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DataStorageInAndroidAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContactImportScreen(contentResolver = contentResolver)
                }
            }
        }
    }
}


// 1. Request data from Contacts using ContentResolver
data class Contact(
    val id: String,
    val name: String,
    var phoneNumbers: List<String> = emptyList(),
    var emails: List<String> = emptyList()
//    val imageUri: Uri? //

)





@Composable
fun ContactImportScreen(contentResolver: ContentResolver) {
    val ctx = LocalContext.current
    var importedContacts by remember { mutableStateOf<List<Contact>>(emptyList()) }

    // Request permission launcher
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, perform the contact import
                importContacts(contentResolver)
            } else {
                // Handle the case where permission is not granted
                Toast.makeText(
                    ctx,
//                    context,
                    "Permission to read contacts is required for this feature.",
                    Toast.LENGTH_SHORT
                ).show()
                // Optionally, you can navigate to settings to let the user grant the permission manually
                navigateToAppSettings(ctx)
            }
        }

    Column {
        Button(onClick = {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)

            // Query all contacts
            val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)

            if (cursor != null) {
                // ... your existing code
                cursor?.use {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val name = cursor.getString(nameIndex)

                        // Create contact object
                        val contact = Contact(id = id, name = name)

                        // Add to list
                        importedContacts = importedContacts + contact
                    }
                }
            } else {
                Log.e("ContactImportScreen", "Cursor is null")
            }


        }) {
            Text("Import Contacts")
        }

        // Display list of imported contacts
        LazyColumn {
            items(importedContacts) { contact ->
                ContactListItem(contact = contact)
            }
        }
    }
}
@Composable
fun ContactListItem(contact: Contact) {
    // Your code for displaying individual contact items
    // e.g.,
    Text("Id: ${contact.id}")
    Text("Name: ${contact.name}")
    Text("Ph#: ${contact.phoneNumbers}")
    Text("Email: ${contact.emails}")
}


fun navigateToAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}


fun importContacts(contentResolver: ContentResolver): List<Contact> {
    Log.d("ContactImportScreen", "inside importContacts")
    val contactsList = mutableListOf<Contact>()

    // Query all contacts
    val cursor = contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        null,
        null,
        null,
        null
    )

    cursor?.use {
        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

        while (cursor.moveToNext()) {
            val contactId = cursor.getString(idIndex)
            val contactName = cursor.getString(nameIndex)

            // Create contact object with basic information
            val contact = Contact(id = contactId, name = contactName)

            // Retrieve phone numbers
            var phoneNumbers = getContactPhoneNumbers(contentResolver, contactId)
            contact.phoneNumbers = phoneNumbers

            // Retrieve emails
            var emails = getContactEmails(contentResolver, contactId)
            contact.emails = emails

            // Add to list
            contactsList.add(contact)
        }
    }

    return contactsList
}

fun getContactPhoneNumbers(contentResolver: ContentResolver, contactId: String): List<String> {
    val phoneNumbers = mutableListOf<String>()
    val phoneCursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null,
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
        arrayOf(contactId),
        null
    )

    phoneCursor?.use {
        val phoneNumberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (it.moveToNext()) {
            val phoneNumber = it.getString(phoneNumberIndex)
            Log.d("getContactPhoneNumbers", "Phone Number: $phoneNumber")
            phoneNumbers.add(phoneNumber)
        }
    }

    return phoneNumbers
}

fun getContactEmails(contentResolver: ContentResolver, contactId: String): List<String> {
    val emails = mutableListOf<String>()
    val emailCursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        null,
        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
        arrayOf(contactId),
        null
    )

    emailCursor?.use {
        val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (it.moveToNext()) {
            val email = it.getString(emailIndex)
            Log.d("getContactEmails", "Email: $email")
            emails.add(email)
        }
    }

    return emails
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val contentResolver: ContentResolver = LocalContext.current.contentResolver
    DataStorageInAndroidAppTheme {
        ContactImportScreen(contentResolver = contentResolver)
    }
}