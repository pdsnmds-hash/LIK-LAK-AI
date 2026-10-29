package bg.liklak.sandbox

/** Organization/contact data supplied by the app owner. Links remain copy-only. */
internal object OrganizationDetails {
 const val studio="MajesticCore Digital Innovation Studio"
 const val organization="Cobbs Creek Realty Solutions LLC"
 const val email="chesesluivuvizcu@gmail.com"
 const val address="628 Coffeen Ave\nSheridan - 82801-5314\nUnited States (US)"
 val contacts get()="$studio\n$organization\n$email\n$address"
}
