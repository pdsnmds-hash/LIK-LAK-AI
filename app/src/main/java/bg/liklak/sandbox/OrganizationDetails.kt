package bg.liklak.sandbox

/** Organization/contact data supplied by the app owner. Links remain copy-only. */
internal object OrganizationDetails {
 const val organization="BizWize Investments, Inc."
 const val email="chesesluivuvizcu@gmail.com"
 const val address="1712 Pioneer Ave Ste 115\nCheyenne - 82001-4406\nUnited States (US)"
 val contacts get()="$organization\n$email\n$address"
}
