package com.mindmatrix.manekelsa

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.net.URLEncoder
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val navy = Color.rgb(21, 18, 74)
    private val green = Color.rgb(7, 132, 61)
    private val muted = Color.rgb(91, 89, 108)
    private val paper = Color.rgb(250, 250, 252)
    private val gold = Color.rgb(246, 178, 26)
    private val workers = emptyList<Worker>()
    private val prefs by lazy { getSharedPreferences("mane_kelsa_state", MODE_PRIVATE) }
    private val cleanAppVersion = 2
    private var activeScreen = Screen.Home
    private var selectedWorkerIndex = 0
    private var pendingPaymentWorkerIndex = -1
    private val upiPaymentRequestCode = 7001
    private val lang: Lang
        get() = if (prefs.getString("language", "kn") == "en") Lang.EN else Lang.KN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetSavedDataOnce()
        if (prefs.getBoolean("logged_in", false)) showHome() else showLogin()
    }

    private fun resetSavedDataOnce() {
        if (prefs.getInt("clean_app_version", 0) >= cleanAppVersion) return
        prefs.edit()
            .clear()
            .putInt("clean_app_version", cleanAppVersion)
            .putString("language", "en")
            .apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != upiPaymentRequestCode) return

        val paidWorker = workerList().getOrNull(pendingPaymentWorkerIndex)
        pendingPaymentWorkerIndex = -1
        if (resultCode == RESULT_OK && isUpiPaymentSuccess(data) && paidWorker != null) {
            Toast.makeText(this, t("Payment successful", "ಪಾವತಿ ಯಶಸ್ವಿಯಾಗಿದೆ"), Toast.LENGTH_SHORT).show()
            showRating(paidWorker)
        } else {
            Toast.makeText(this, t("Payment not confirmed", "ಪಾವತಿ ದೃಢೀಕರಿಸಲಾಗಿಲ್ಲ"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogin() {
        activeScreen = Screen.Login
        setContent(t("Choose Login Type", "ಲಾಗಿನ್ ಪ್ರಕಾರ ಆರಿಸಿ"), t("Select your role to continue.", "ಮುಂದುವರಿಯಲು ನಿಮ್ಮ ಪಾತ್ರವನ್ನು ಆರಿಸಿ."), false) { body ->
            body.addView(roleChoiceCard())
        }
    }

    private fun showCustomerLogin() {
        activeScreen = Screen.CustomerLogin
        setContent(t("Customer Login", "ಗ್ರಾಹಕ ಲಾಗಿನ್"), t("Login to find and contact nearby workers.", "ಹತ್ತಿರದ ಕೆಲಸಗಾರರನ್ನು ಹುಡುಕಿ ಸಂಪರ್ಕಿಸಲು ಲಾಗಿನ್ ಮಾಡಿ."), false) { body ->
            body.addView(loginHeader("👤", t("Welcome Customer", "ಸ್ವಾಗತ ಗ್ರಾಹಕರೇ"), t("Enter username/mobile number and password.", "ಬಳಕೆದಾರ ಹೆಸರು/ಮೊಬೈಲ್ ಸಂಖ್ಯೆ ಮತ್ತು ಪಾಸ್‌ವರ್ಡ್ ನಮೂದಿಸಿ.")))
            val phone = EditText(this).apply {
                hint = t("Username or mobile number", "ಬಳಕೆದಾರ ಹೆಸರು ಅಥವಾ ಮೊಬೈಲ್ ಸಂಖ್ಯೆ")
                setText(prefs.getString("user_phone", ""))
                styleInput()
            }
            val password = passwordField()
            body.addView(phone, matchWrap())
            body.addView(password, matchWrap())
            primaryButton(t("Customer Login", "ಗ್ರಾಹಕ ಲಾಗಿನ್")) {
                val savedPhone = prefs.getString("user_phone", "")
                val savedPassword = prefs.getString("user_password", "")
                if (phone.text.isBlank()) {
                    Toast.makeText(this, t("Please enter phone number", "ದಯವಿಟ್ಟು ಫೋನ್ ಸಂಖ್ಯೆ ನಮೂದಿಸಿ"), Toast.LENGTH_SHORT).show()
                } else if (password.text.isBlank() || phone.text.toString() != savedPhone || password.text.toString() != savedPassword) {
                    Toast.makeText(this, t("Password incorrect", "ಪಾಸ್‌ವರ್ಡ್ ತಪ್ಪಾಗಿದೆ"), Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit()
                        .putBoolean("logged_in", true)
                        .putString("user_type", "customer")
                        .putString("user_phone", phone.text.toString())
                        .apply()
                    showHome()
                }
            }.also(body::addView)
            secondaryButton(t("New customer? Register", "ಹೊಸ ಗ್ರಾಹಕರೇ? ನೋಂದಾಯಿಸಿ")) { showCreateAccount() }.also(body::addView)
            secondaryButton(t("Back", "ಹಿಂತಿರುಗಿ")) { showLogin() }.also(body::addView)
        }
    }

    private fun showWorkerLogin() {
        activeScreen = Screen.WorkerLogin
        setContent(t("Worker Login", "ಕೆಲಸಗಾರ ಲಾಗಿನ್"), t("Login to manage profile, availability, and calls.", "ಪ್ರೊಫೈಲ್, ಲಭ್ಯತೆ ಮತ್ತು ಕರೆಗಳನ್ನು ನಿರ್ವಹಿಸಲು ಲಾಗಿನ್ ಮಾಡಿ."), false) { body ->
            body.addView(loginHeader("🧑", t("Welcome Worker", "ಸ್ವಾಗತ ಕೆಲಸಗಾರರೇ"), t("Manage your worker profile after login.", "ಲಾಗಿನ್ ನಂತರ ನಿಮ್ಮ ಕೆಲಸಗಾರ ಪ್ರೊಫೈಲ್ ನಿರ್ವಹಿಸಿ.")))
            val phone = EditText(this).apply {
                hint = t("Worker mobile number", "ಕೆಲಸಗಾರರ ಮೊಬೈಲ್ ಸಂಖ್ಯೆ")
                setText(prefs.getString("worker_phone", ""))
                styleInput()
            }
            val password = passwordField()
            body.addView(phone, matchWrap())
            body.addView(password, matchWrap())
            primaryButton(t("Worker Login", "ಕೆಲಸಗಾರ ಲಾಗಿನ್")) {
                val savedPhone = prefs.getString("worker_phone", "")
                val savedPassword = prefs.getString("worker_password", "")
                if (phone.text.isBlank()) {
                    Toast.makeText(this, t("Please enter worker mobile number", "ದಯವಿಟ್ಟು ಕೆಲಸಗಾರರ ಮೊಬೈಲ್ ಸಂಖ್ಯೆ ನಮೂದಿಸಿ"), Toast.LENGTH_SHORT).show()
                } else if (password.text.isBlank() || phone.text.toString() != savedPhone || password.text.toString() != savedPassword) {
                    Toast.makeText(this, t("Password incorrect", "ಪಾಸ್‌ವರ್ಡ್ ತಪ್ಪಾಗಿದೆ"), Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit()
                        .putBoolean("logged_in", true)
                        .putString("user_type", "worker")
                        .putString("worker_phone", phone.text.toString())
                        .apply()
                    if (prefs.getBoolean("worker_registered", false)) showHome() else showWorkerRegistration()
                }
            }.also(body::addView)
            secondaryButton(t("New Worker? Register", "ಹೊಸ ಕೆಲಸಗಾರರೇ? ನೋಂದಾಯಿಸಿ")) { showWorkerRegistration() }.also(body::addView)
            secondaryButton(t("Back", "ಹಿಂತಿರುಗಿ")) { showLogin() }.also(body::addView)
        }
    }

    private fun showCreateAccount() {
        activeScreen = Screen.CreateAccount
        setContent(t("Create Customer Account", "ಗ್ರಾಹಕ ಖಾತೆ ರಚಿಸಿ"), t("Register as a customer to contact local workers.", "ಸ್ಥಳೀಯ ಕೆಲಸಗಾರರನ್ನು ಸಂಪರ್ಕಿಸಲು ಗ್ರಾಹಕರಾಗಿ ನೋಂದಾಯಿಸಿ."), false) { body ->
            val name = EditText(this).apply {
                hint = t("Full name", "ಪೂರ್ಣ ಹೆಸರು")
                styleInput()
            }
            val phone = EditText(this).apply {
                hint = t("Phone number", "ಫೋನ್ ಸಂಖ್ಯೆ")
                styleInput()
            }
            val password = passwordField()
            body.addView(name, matchWrap())
            body.addView(phone, matchWrap())
            body.addView(password, matchWrap())
            primaryButton(t("Create Customer Account", "ಗ್ರಾಹಕ ಖಾತೆ ರಚಿಸಿ")) {
                if (name.text.isBlank() || phone.text.isBlank() || password.text.isBlank()) {
                    Toast.makeText(this, t("Please fill name, phone, and password", "ಹೆಸರು, ಫೋನ್ ಮತ್ತು ಪಾಸ್‌ವರ್ಡ್ ತುಂಬಿರಿ"), Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit()
                        .putBoolean("logged_in", true)
                        .putString("user_type", "customer")
                        .putString("user_name", name.text.toString())
                        .putString("user_phone", phone.text.toString())
                        .putString("user_password", password.text.toString())
                        .apply()
                    Toast.makeText(this, t("Account created", "ಖಾತೆ ರಚಿಸಲಾಗಿದೆ"), Toast.LENGTH_SHORT).show()
                    showHome()
                }
            }.also(body::addView)
            secondaryButton(t("Already have account? Customer Login", "ಈಗಾಗಲೇ ಖಾತೆ ಇದೆಯೇ? ಗ್ರಾಹಕ ಲಾಗಿನ್")) { showCustomerLogin() }.also(body::addView)
        }
    }

    private fun showWorkerRegistration() {
        activeScreen = Screen.WorkerRegister
        setContent(t("Register as Worker", "ಕೆಲಸಗಾರರಾಗಿ ನೋಂದಾಯಿಸಿ"), t("Add your phone, address, skill, and payment range so customers can contact you.", "ಗ್ರಾಹಕರು ಸಂಪರ್ಕಿಸಲು ಫೋನ್, ವಿಳಾಸ, ಕೌಶಲ್ಯ ಮತ್ತು ಪಾವತಿ ಶ್ರೇಣಿ ಸೇರಿಸಿ."), false) { body ->
            val name = EditText(this).apply {
                hint = t("Worker name", "ಕೆಲಸಗಾರರ ಹೆಸರು")
                setText(prefs.getString("worker_name", ""))
                styleInput()
            }
            val phone = EditText(this).apply {
                hint = t("Mobile number", "ಮೊಬೈಲ್ ಸಂಖ್ಯೆ")
                setText(prefs.getString("worker_phone", ""))
                styleInput()
            }
            val address = EditText(this).apply {
                hint = t("Address / Street / Area", "ವಿಳಾಸ / ರಸ್ತೆ / ಪ್ರದೇಶ")
                setText(prefs.getString("worker_address", ""))
                minLines = 2
                styleInput()
            }
            val skill = EditText(this).apply {
                hint = t("Skill example: Cleaning, Gardening", "ಕೌಶಲ್ಯ ಉದಾ: ಕ್ಲೀನಿಂಗ್, ತೋಟಗಾರಿಕೆ")
                setText(prefs.getString("worker_skill", ""))
                styleInput()
            }
            val rate = EditText(this).apply {
                hint = t("Payment range example: 100-200", "ಪಾವತಿ ಶ್ರೇಣಿ ಉದಾ: 100-200")
                setText(prefs.getString("worker_rate", ""))
                styleInput()
            }
            val password = passwordField()
            body.addView(name, matchWrap())
            body.addView(phone, matchWrap())
            body.addView(address, matchWrap())
            body.addView(skill, matchWrap())
            body.addView(rate, matchWrap())
            body.addView(password, matchWrap())
            body.addView(card {
                addView(label(t("Customer contact preview", "ಗ್ರಾಹಕರಿಗೆ ಕಾಣುವ ಮಾಹಿತಿ"), 17, navy, true))
                addView(small(t("Name, skill, phone number, address, payment range, and call button will be visible in Nearby Workers.", "ಹೆಸರು, ಕೌಶಲ್ಯ, ಫೋನ್ ಸಂಖ್ಯೆ, ವಿಳಾಸ, ಪಾವತಿ ಶ್ರೇಣಿ ಮತ್ತು ಕರೆ ಬಟನ್ ಹತ್ತಿರದ ಕೆಲಸಗಾರರಲ್ಲಿ ಕಾಣುತ್ತದೆ.")))
            })
            primaryButton(t("Save Worker Registration", "ಕೆಲಸಗಾರರ ನೋಂದಣಿ ಉಳಿಸಿ")) {
                if (name.text.isBlank() || phone.text.isBlank() || address.text.isBlank() || skill.text.isBlank() || rate.text.isBlank() || password.text.isBlank()) {
                    Toast.makeText(this, t("Please fill all worker details", "ದಯವಿಟ್ಟು ಎಲ್ಲಾ ಕೆಲಸಗಾರರ ವಿವರಗಳನ್ನು ತುಂಬಿರಿ"), Toast.LENGTH_SHORT).show()
                } else if (paymentRange(rate.text.toString()) == null) {
                    Toast.makeText(this, t("Enter payment range like 100-200", "100-200 ರೀತಿಯಲ್ಲಿ ಪಾವತಿ ಶ್ರೇಣಿ ನಮೂದಿಸಿ"), Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit()
                        .putBoolean("logged_in", true)
                        .putString("user_type", "worker")
                        .putBoolean("worker_registered", true)
                        .putString("worker_name", name.text.toString())
                        .putString("worker_phone", phone.text.toString())
                        .putString("worker_address", address.text.toString())
                        .putString("worker_skill", skill.text.toString())
                        .putString("worker_rate", rate.text.toString())
                        .putString("worker_password", password.text.toString())
                        .apply()
                    selectedWorkerIndex = 0
                    Toast.makeText(this, t("Worker registered successfully", "ಕೆಲಸಗಾರರ ನೋಂದಣಿ ಯಶಸ್ವಿಯಾಗಿದೆ"), Toast.LENGTH_SHORT).show()
                    showHome()
                }
            }.also(body::addView)
            secondaryButton(t("Back to Worker Login", "ಕೆಲಸಗಾರ ಲಾಗಿನ್‌ಗೆ ಹಿಂತಿರುಗಿ")) { showWorkerLogin() }.also(body::addView)
        }
    }

    private fun showHome() {
        activeScreen = Screen.Home
        setContent(t("Mane-Kelsa", "ಮನೆ-ಕೆಲಸ\nMane-Kelsa"), t("Local work. Local people.", "ಸ್ಥಳೀಯ ಕೆಲಸ. ಸ್ಥಳೀಯ ಜನ.")) { body ->
            body.addView(currentUserCard())
            hero(body)
            if (isCurrentUserWorker()) {
                primaryButton(t("Update Availability", "ಲಭ್ಯತೆ ನವೀಕರಿಸಿ")) { showAvailability() }.also(body::addView)
            } else {
                primaryButton(t("I Need Help", "ನನಗೆ ಸಹಾಯ ಬೇಕು")) { showNearby() }.also(body::addView)
            }
            sectionTitle(body, t("Quick Info", "ತ್ವರಿತ ಮಾಹಿತಿ"))
            val row = row()
            row.addView(infoTile("👥", t("Workers", "ಕೆಲಸಗಾರರು"), workerList().size.toString()))
            row.addView(infoTile("💼", t("Jobs Done", "ಕೆಲಸ ಮುಗಿದಿದೆ"), "0"))
            row.addView(infoTile("★", t("Rating", "ರೇಟಿಂಗ್"), "0.0"))
            row.addView(infoTile("📍", t("Nearby", "ಹತ್ತಿರ"), if (workerList().isEmpty()) "0" else "1"))
            body.addView(row)
            sectionTitle(body, t("Today's Status", "ಇಂದಿನ ಸ್ಥಿತಿ"))
            body.addView(card {
                addView(label("${t("Availability", "ಲಭ್ಯತೆ")}: ${if (isAvailable()) t("Online", "ಆನ್‌ಲೈನ್") else t("Offline", "ಆಫ್‌ಲೈನ್")}", 18, green, true))
                addView(small("${t("Work duration", "ಕೆಲಸದ ಅವಧಿ")}: ${selectedSlotText()}"))
                addView(small(t("Your saved details work even when internet is not available.", "ಉಳಿಸಿದ ಮಾಹಿತಿ ನೆಟ್ ಇಲ್ಲದಿದ್ದರೂ ಫೋನ್‌ನಲ್ಲಿ ಕೆಲಸ ಮಾಡುತ್ತದೆ.")))
            })
            dangerButton(t("Logout", "ಲಾಗ್ ಔಟ್")) { confirmLogout() }.also(body::addView)
        }
    }

    private fun showProfile(worker: Worker) {
        selectedWorkerIndex = workerList().indexOf(worker).coerceAtLeast(0)
        activeScreen = Screen.Profile
        setContent(t("Worker Profile", "ಕೆಲಸಗಾರ ಪ್ರೊಫೈಲ್"), worker.name(lang)) { body ->
            body.addView(avatar(worker.icon, 96))
            body.addView(centerTitle(worker.name(lang), 22))
            body.addView(centerTitle(worker.role(lang), 16, muted))
            body.addView(centerTitle(stars(worker.rating) + "  ${worker.rating} (28)", 16, gold))
            sectionTitle(body, t("Skills", "ಕೌಶಲ್ಯಗಳು"))
            val skills = row()
            listOf(worker.skill(lang), t("Cleaning", "ಕ್ಲೀನಿಂಗ್"), t("Painting", "ಪೇಂಟಿಂಗ್")).forEach { skills.addView(chip(it)) }
            body.addView(skills)
            body.addView(detail(t("Payment Range", "ಪಾವತಿ ಶ್ರೇಣಿ"), worker.rate(lang)))
            body.addView(detail(t("Experience", "ಅನುಭವ"), t("5+ Years", "5+ ವರ್ಷ")))
            body.addView(detail(t("Phone Number", "ಫೋನ್ ಸಂಖ್ಯೆ"), worker.phone))
            body.addView(detail(t("Address", "ವಿಳಾಸ"), worker.area(lang)))
            if (isCurrentUserWorker()) {
                primaryButton(t("Update Availability", "ಲಭ್ಯತೆ ನವೀಕರಿಸಿ")) { showAvailability() }.also(body::addView)
            } else {
                primaryButton(t("Call Now", "ಕರೆ ಮಾಡಿ")) { showCall(worker) }.also(body::addView)
                secondaryButton(t("Book & Pay", "ಬುಕ್ ಮಾಡಿ ಮತ್ತು ಪಾವತಿಸಿ")) { showPayment(worker) }.also(body::addView)
            }
        }
    }

    private fun showAvailability() {
        activeScreen = Screen.Availability
        setContent(t("Availability", "ಲಭ್ಯತೆ"), t("Are you ready for work today?", "ಇಂದು ಕೆಲಸಕ್ಕೆ ಸಿದ್ಧವೇ?")) { body ->
            val status = label("", 18, green, true)
            val availabilitySwitch = Switch(this).apply {
                text = t("I am available today", "ನಾನು ಇಂದು ಲಭ್ಯವಿದ್ದೇನೆ")
                textSize = 17f
                setTextColor(green)
                isChecked = isAvailable()
                setPadding(dp(4), dp(8), dp(4), dp(8))
            }
            fun refreshStatus() {
                status.text = if (availabilitySwitch.isChecked) {
                    t("Your status is visible as Online instantly.", "ನಿಮ್ಮ ಸ್ಥಿತಿ ತಕ್ಷಣ ಆನ್‌ಲೈನ್ ಆಗಿ ಕಾಣುತ್ತದೆ.")
                } else {
                    t("Your status is now Offline.", "ನಿಮ್ಮ ಸ್ಥಿತಿ ಈಗ ಆಫ್‌ಲೈನ್ ಆಗಿದೆ.")
                }
            }
            availabilitySwitch.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                prefs.edit().putBoolean("available", checked).apply()
                refreshStatus()
            }
            refreshStatus()
            body.addView(availabilitySwitch)
            body.addView(status)
            sectionTitle(body, t("Select Availability", "ಕೆಲಸದ ಸಮಯ ಆರಿಸಿ"))
            val group = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
            listOf(Slot.TwoHours, Slot.HalfDay, Slot.FullDay).forEach { slot ->
                group.addView(RadioButton(this).apply {
                    text = slot.label(lang)
                    textSize = 16f
                    id = View.generateViewId()
                    isChecked = prefs.getString("slot", Slot.TwoHours.key) == slot.key
                    setPadding(0, dp(8), 0, dp(8))
                    setOnClickListener { prefs.edit().putString("slot", slot.key).apply() }
                })
            }
            body.addView(group)
            body.addView(detail(t("Preferred Time", "ಆದ್ಯತೆಯ ಸಮಯ"), "9:00 AM - 4:00 PM"))
            val note = EditText(this).apply {
                hint = t("Add a note...", "ಟಿಪ್ಪಣಿ ಬರೆಯಿರಿ...")
                minLines = 3
                setText(prefs.getString("note", ""))
                styleInput()
            }
            body.addView(note, matchWrap())
            primaryButton(t("Save", "ಉಳಿಸಿ")) {
                prefs.edit().putString("note", note.text.toString()).apply()
                Toast.makeText(this, t("Availability saved", "ಲಭ್ಯತೆ ಉಳಿಸಲಾಗಿದೆ"), Toast.LENGTH_SHORT).show()
                showHome()
            }.also(body::addView)
        }
    }

    private fun showNearby() {
        activeScreen = Screen.Nearby
        setContent(t("Nearby Workers", "ಹತ್ತಿರದ ಕೆಲಸಗಾರರು"), t("Near: 2nd Cross, Hennur", "ಹತ್ತಿರ: 2ನೇ ಕ್ರಾಸ್, ಹೆಣ್ಣೂರು")) { body ->
            val availableWorkers = workerList().sortedBy { it.distanceKm }
            if (availableWorkers.isEmpty()) {
                body.addView(card {
                    gravity = Gravity.CENTER_HORIZONTAL
                    addView(avatar("👤", 72))
                    addView(centerTitle(t("No workers added yet", "ಇನ್ನೂ ಕೆಲಸಗಾರರನ್ನು ಸೇರಿಸಲಾಗಿಲ್ಲ"), 20, navy))
                    addView(centerTitle(t("Register a worker account to show worker details here.", "ಇಲ್ಲಿ ಕೆಲಸಗಾರರ ವಿವರ ತೋರಿಸಲು ಕೆಲಸಗಾರ ಖಾತೆ ನೋಂದಾಯಿಸಿ."), 14, muted))
                    primaryButton(t("Create Worker Account", "ಕೆಲಸಗಾರ ಖಾತೆ ರಚಿಸಿ")) { showWorkerRegistration() }.also(::addView)
                })
                return@setContent
            }
            availableWorkers.forEach { worker ->
                body.addView(card {
                    val item = row(Gravity.CENTER_VERTICAL)
                    item.addView(avatar(worker.icon, 58))
                    val details = column().apply {
                        addView(label(worker.name(lang), 17, Color.BLACK, true))
                        addView(small("${worker.role(lang)} • ${worker.rate(lang)}"))
                        addView(small(worker.area(lang)))
                        addView(small("${t("Phone", "ಫೋನ್")}: ${worker.phone}"))
                    }
                    item.addView(details, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    val score = column(Gravity.END).apply {
                        addView(label(worker.rating.toString(), 16, green, true))
                        addView(small("${worker.distanceKm} km"))
                    }
                    item.addView(score)
                    setOnClickListener { showProfile(worker) }
                    addView(item)
                })
            }
        }
    }

    private fun showCall(worker: Worker) {
        selectedWorkerIndex = workerList().indexOf(worker).coerceAtLeast(0)
        activeScreen = Screen.Call
        setContent(t("Call Worker", "ಕೆಲಸಗಾರರಿಗೆ ಕರೆ"), worker.name(lang)) { body ->
            body.addView(centerTitle("☎", 72, green))
            body.addView(centerTitle(t("Calling...", "ಕರೆ ಮಾಡಲಾಗುತ್ತಿದೆ..."), 16, muted))
            body.addView(centerTitle(worker.name(lang), 24, Color.BLACK))
            body.addView(centerTitle(worker.role(lang), 17, muted))
            body.addView(centerTitle(worker.rate(lang), 17, Color.BLACK))
            body.addView(centerTitle(worker.phone, 17, navy))
            primaryButton(t("Open Phone Dialer", "ಫೋನ್ ಡಯಲರ್ ತೆರೆಯಿರಿ")) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.phone}")))
            }.also(body::addView)
            dangerButton(t("End Call", "ಕರೆ ಮುಗಿಸಿ")) { showRating(worker) }.also(body::addView)
            secondaryButton(t("Proceed to Payment", "ಪಾವತಿಗೆ ಮುಂದುವರಿಸಿ")) { showPayment(worker) }.also(body::addView)
        }
    }

    private fun showPayment(worker: Worker) {
        selectedWorkerIndex = workerList().indexOf(worker).coerceAtLeast(0)
        activeScreen = Screen.Payment
        setContent(t("Payment", "ಪಾವತಿ"), t("Pay safely after confirming the worker.", "ಕೆಲಸಗಾರರನ್ನು ಖಚಿತಪಡಿಸಿ ಸುರಕ್ಷಿತವಾಗಿ ಪಾವತಿಸಿ.")) { body ->
            body.addView(card {
                addView(label(worker.name(lang), 20, Color.BLACK, true))
                addView(small("${worker.role(lang)} • ${worker.rate(lang)}"))
                addView(small(worker.area(lang)))
            })
            sectionTitle(body, t("Select Payment Method", "ಪಾವತಿ ವಿಧಾನ ಆರಿಸಿ"))
            val methods = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
            val cashId = View.generateViewId()
            val upiId = View.generateViewId()
            methods.addView(RadioButton(this).apply {
                text = t("Cash after work", "ಕೆಲಸದ ನಂತರ ನಗದು")
                textSize = 16f
                id = cashId
                isChecked = true
                setPadding(0, dp(8), 0, dp(8))
            })
            methods.addView(RadioButton(this).apply {
                text = "UPI"
                textSize = 16f
                id = upiId
                setPadding(0, dp(8), 0, dp(8))
            })
            val amountInput = EditText(this).apply {
                hint = t("Enter amount to pay (${worker.rate(lang)})", "ಪಾವತಿಸಬೇಕಾದ ಮೊತ್ತ ನಮೂದಿಸಿ (${worker.rate(lang)})")
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(paymentAmount(worker))
                styleInput()
            }
            val upiInput = EditText(this).apply {
                hint = t("Worker UPI ID example: name@upi", "ಕೆಲಸಗಾರರ UPI ID ಉದಾ: name@upi")
                setText(paymentUpiId(worker))
                styleInput()
                visibility = View.GONE
            }
            val amountStatus = detail(t("Amount given", "ಕೊಟ್ಟ ಮೊತ್ತ"), amountInput.text.toString())
            val payButton = primaryButton(t("Confirm Cash Payment", "ನಗದು ಪಾವತಿ ದೃಢೀಕರಿಸಿ")) {}
            fun refreshAmountStatus(checkedId: Int = methods.checkedRadioButtonId) {
                val selectedAmount = amountInput.text.toString()
                if (checkedId == upiId) {
                    upiInput.visibility = View.VISIBLE
                    amountStatus.removeAllViews()
                    amountStatus.addView(label(t("Amount to pay", "ಪಾವತಿಸಬೇಕಾದ ಮೊತ್ತ"), 14, Color.BLACK, true))
                    amountStatus.addView(label(selectedAmount, 16, Color.BLACK, false))
                    payButton.text = t("Pay with UPI App", "UPI ಆಪ್ ಮೂಲಕ ಪಾವತಿಸಿ")
                } else {
                    upiInput.visibility = View.GONE
                    amountStatus.removeAllViews()
                    amountStatus.addView(label(t("Amount given", "ಕೊಟ್ಟ ಮೊತ್ತ"), 14, Color.BLACK, true))
                    amountStatus.addView(label(selectedAmount, 16, Color.BLACK, false))
                    payButton.text = t("Confirm Cash Payment", "ನಗದು ಪಾವತಿ ದೃಢೀಕರಿಸಿ")
                }
            }
            methods.setOnCheckedChangeListener { _, checkedId -> refreshAmountStatus(checkedId) }
            amountInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = refreshAmountStatus()
                override fun afterTextChanged(s: Editable?) = Unit
            })
            payButton.setOnClickListener {
                val amount = amountInput.text.toString()
                if (!isValidPaymentAmount(amount)) {
                    Toast.makeText(this, t("Enter a valid amount", "ಸರಿಯಾದ ಮೊತ್ತ ನಮೂದಿಸಿ"), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (methods.checkedRadioButtonId == upiId) {
                    val workerUpiId = upiInput.text.toString().trim()
                    if (!isValidUpiId(workerUpiId)) {
                        Toast.makeText(this, t("Enter a valid UPI ID like name@upi", "name@upi ರೀತಿಯಲ್ಲಿ ಸರಿಯಾದ UPI ID ನಮೂದಿಸಿ"), Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    savePayment(worker, amount, "UPI", workerUpiId)
                    launchUpiPayment(worker, amount, workerUpiId)
                } else {
                    savePayment(worker, amount, "Cash after work", t("Amount given: $amount", "ಕೊಟ್ಟ ಮೊತ್ತ: $amount"))
                    Toast.makeText(this, t("Amount given: $amount", "ಕೊಟ್ಟ ಮೊತ್ತ: $amount"), Toast.LENGTH_SHORT).show()
                    showRating(worker)
                }
            }
            body.addView(methods)
            body.addView(amountInput, matchWrap())
            body.addView(upiInput, matchWrap())
            body.addView(amountStatus)
            body.addView(payButton)
        }
    }

    private fun showRating(worker: Worker) {
        selectedWorkerIndex = workerList().indexOf(worker).coerceAtLeast(0)
        activeScreen = Screen.Rating
        setContent(t("Rating", "ರೇಟಿಂಗ್"), worker.name(lang)) { body ->
            body.addView(centerTitle("👍", 76, navy))
            body.addView(centerTitle(t("How was your experience?", "ನಿಮ್ಮ ಅನುಭವ ಹೇಗಿತ್ತು?"), 20, Color.BLACK))
            body.addView(centerTitle("★★★★★", 34, gold))
            val review = EditText(this).apply {
                hint = t("Great work!", "ಚೆನ್ನಾದ ಕೆಲಸ!")
                minLines = 3
                styleInput()
            }
            body.addView(review, matchWrap())
            primaryButton(t("Submit", "ಸಲ್ಲಿಸಿ")) {
                Toast.makeText(this, t("Thank you! Rating saved.", "ಧನ್ಯವಾದಗಳು! ರೇಟಿಂಗ್ ಉಳಿಸಲಾಗಿದೆ."), Toast.LENGTH_SHORT).show()
                showNearby()
            }.also(body::addView)
        }
    }

    private fun setContent(title: String, subtitle: String, showNav: Boolean = true, content: (LinearLayout) -> Unit) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(paper)
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        }
        root.addView(topBar(title))
        val scroll = ScrollView(this)
        val body = column().apply { setPadding(dp(18), dp(16), dp(18), dp(22)) }
        body.addView(languageButtons())
        body.addView(label(title, 22, navy, true).apply { setPadding(0, dp(4), 0, 0) })
        body.addView(label(subtitle, 14, muted, false).apply { setPadding(0, dp(2), 0, dp(6)) })
        content(body)
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (showNav) root.addView(bottomNav())
        setContentView(root)
    }

    private fun languageButtons(): LinearLayout = row().apply {
        setPadding(0, 0, 0, dp(12))
        addView(languageButton("ಕನ್ನಡ", Lang.KN), LinearLayout.LayoutParams(0, dp(42), 1f).apply { setMargins(0, 0, dp(6), 0) })
        addView(languageButton("English", Lang.EN), LinearLayout.LayoutParams(0, dp(42), 1f).apply { setMargins(dp(6), 0, 0, 0) })
    }

    private fun languageButton(text: String, value: Lang): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        setTextColor(if (lang == value) Color.WHITE else navy)
        background = getDrawable(if (lang == value) R.drawable.primary_button else R.drawable.secondary_button)
        setOnClickListener {
            prefs.edit().putString("language", if (value == Lang.EN) "en" else "kn").apply()
            redrawCurrentScreen()
        }
    }

    private fun procedureCard(): LinearLayout = card {
        addView(small(t("1. Choose your language using the Kannada / English buttons.", "1. ಕನ್ನಡ / English ಬಟನ್ ಬಳಸಿ ಭಾಷೆ ಆರಿಸಿ.")))
        addView(small(t("2. Workers update availability and work timing.", "2. ಕೆಲಸಗಾರರು ಲಭ್ಯತೆ ಮತ್ತು ಸಮಯವನ್ನು ನವೀಕರಿಸುತ್ತಾರೆ.")))
        addView(small(t("3. Residents open Nearby Workers and call directly.", "3. ನಿವಾಸಿಗಳು ಹತ್ತಿರದ ಕೆಲಸಗಾರರನ್ನು ನೋಡಿ ನೇರವಾಗಿ ಕರೆ ಮಾಡುತ್ತಾರೆ.")))
        addView(small(t("4. After work, give a simple rating to build trust.", "4. ಕೆಲಸದ ನಂತರ ರೇಟಿಂಗ್ ನೀಡಿ ನಂಬಿಕೆ ಹೆಚ್ಚಿಸಿ.")))
    }

    private fun roleChoiceCard(): LinearLayout = card {
        addView(centerTitle(t("Login as", "ಲಾಗಿನ್ ಆಗಿ"), 18, navy))
        addView(roleButton(t("Customer", "ಗ್ರಾಹಕ"), "👤") { showCustomerLogin() }, LinearLayout.LayoutParams(-1, dp(56)).apply {
            setMargins(0, dp(12), 0, 0)
        })
        addView(roleButton(t("Worker", "ಕೆಲಸಗಾರ"), "🧑") { showWorkerLogin() }, LinearLayout.LayoutParams(-1, dp(56)).apply {
            setMargins(0, dp(10), 0, dp(4))
        })
    }

    private fun roleButton(text: String, icon: String, click: () -> Unit): Button =
        Button(this).apply {
            this.text = "$icon  $text"
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            setTextColor(Color.WHITE)
            background = getDrawable(R.drawable.primary_button)
            setOnClickListener { click() }
        }

    private fun loginProfileCard(icon: String, title: String, subtitle: String, click: () -> Unit): LinearLayout =
        card {
            gravity = Gravity.CENTER_HORIZONTAL
            addView(avatar(icon, 56))
            addView(centerTitle(title, 18, navy))
            addView(centerTitle(subtitle, 14, muted))
            primaryButton(t("Login", "ಲಾಗಿನ್"), click).also(::addView)
        }

    private fun loginHeader(icon: String, title: String, subtitle: String): LinearLayout =
        column(Gravity.CENTER).apply {
            setPadding(0, dp(8), 0, dp(12))
            addView(avatar(icon, 72))
            addView(centerTitle(title, 22, navy))
            addView(centerTitle(subtitle, 15, muted))
        }

    private fun currentUserCard(): LinearLayout {
        val isWorker = prefs.getString("user_type", "customer") == "worker"
        val name = if (isWorker) {
            prefs.getString("worker_name", null)?.takeIf { it.isNotBlank() } ?: t("Worker", "ಕೆಲಸಗಾರ")
        } else {
            prefs.getString("user_name", null)?.takeIf { it.isNotBlank() } ?: t("Customer", "ಗ್ರಾಹಕ")
        }
        val phone = if (isWorker) prefs.getString("worker_phone", "") else prefs.getString("user_phone", "")
        return card {
            val profile = row(Gravity.CENTER_VERTICAL)
            profile.addView(avatar(if (isWorker) "🧑" else "👤", 64))
            val details = column().apply {
                addView(label(name, 18, navy, true))
                addView(small(if (isWorker) t("Worker Profile", "ಕೆಲಸಗಾರ ಪ್ರೊಫೈಲ್") else t("Customer Profile", "ಗ್ರಾಹಕ ಪ್ರೊಫೈಲ್")))
                if (!phone.isNullOrBlank()) addView(small("${t("Phone", "ಫೋನ್")}: $phone"))
            }
            profile.addView(details, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(profile)
        }
    }

    private fun topBar(title: String): LinearLayout = row(Gravity.CENTER_VERTICAL).apply {
        setPadding(dp(12), dp(10), dp(12), dp(8))
        setBackgroundColor(Color.WHITE)
        addView(navButton("‹") { goBack() })
        addView(label(title, 16, navy, true), LinearLayout.LayoutParams(0, -2, 1f))
        addView(navButton(if (prefs.getBoolean("logged_in", false)) "⎋" else "⋮") {
            if (prefs.getBoolean("logged_in", false)) confirmLogout()
            else Toast.makeText(this@MainActivity, t("Menu", "ಮೆನು"), Toast.LENGTH_SHORT).show()
        })
    }

    private fun bottomNav(): LinearLayout = row(Gravity.CENTER).apply {
        setPadding(dp(4), dp(8), dp(4), dp(10))
        setBackgroundColor(Color.WHITE)
        addView(navItem("⌂", t("Home", "ಮುಖಪುಟ"), activeScreen == Screen.Home) { showHome() })
        addView(navItem("👤", t("Profile", "ಪ್ರೊಫೈಲ್"), activeScreen == Screen.Profile) {
            selectedWorkerOrNull()?.let { showProfile(it) } ?: showWorkerRegistration()
        })
        addView(navItem("⌖", t("Nearby", "ಹತ್ತಿರ"), activeScreen == Screen.Nearby) { showNearby() })
        addView(navItem("₹", t("Pay", "ಪಾವತಿ"), activeScreen == Screen.Payment) {
            selectedWorkerOrNull()?.let { showPayment(it) } ?: showNearby()
        })
    }

    private fun hero(parent: LinearLayout) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = getDrawable(R.drawable.hero_bg)
            setPadding(dp(16), dp(20), dp(16), dp(20))
            addView(centerTitle("👨", 56, navy))
            addView(centerTitle(t("Find trusted local work today", "ಇಂದು ಹತ್ತಿರದಲ್ಲೇ ನಂಬಿಕೆಯ ಕೆಲಸ ಹುಡುಕಿ"), 17, navy))
        }, matchWrap())
    }

    private fun infoTile(icon: String, title: String, value: String): LinearLayout =
        column(Gravity.CENTER).apply {
            background = getDrawable(R.drawable.card_bg)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            addView(centerTitle(icon, 28, navy))
            addView(centerTitle(title, 12, muted))
            addView(centerTitle(value, 16, Color.BLACK))
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
        }

    private fun passwordField(): EditText =
        EditText(this).apply {
            hint = t("Password", "ಪಾಸ್‌ವರ್ಡ್")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            styleInput()
            setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_view, 0)
            compoundDrawablePadding = dp(8)

            var passwordVisible = false
            setOnTouchListener { view, event ->
                val eyeIcon = compoundDrawables[2]
                if (event.action == MotionEvent.ACTION_UP && eyeIcon != null) {
                    val eyeStart = width - paddingRight - eyeIcon.bounds.width()
                    if (event.x >= eyeStart) {
                        passwordVisible = !passwordVisible
                        inputType = InputType.TYPE_CLASS_TEXT or if (passwordVisible) {
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        } else {
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
                        }
                        setSelection(text.length)
                        view.performClick()
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }

    private fun detail(name: String, value: String): LinearLayout =
        column().apply {
            setPadding(0, dp(8), 0, dp(8))
            addView(label(name, 14, Color.BLACK, true))
            addView(label(value, 16, Color.BLACK, false))
        }

    private fun card(children: LinearLayout.() -> Unit): LinearLayout =
        column().apply {
            background = getDrawable(R.drawable.card_bg)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            children()
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                setMargins(0, dp(9), 0, dp(9))
            }
        }

    private fun avatar(icon: String, size: Int): TextView =
        centerTitle(icon, (size * 0.54).roundToInt(), navy).apply {
            background = getDrawable(R.drawable.avatar_bg)
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, dp(8), 0, dp(8))
            }
        }

    private fun chip(text: String): TextView =
        label(text, 14, Color.BLACK, false).apply {
            background = getDrawable(R.drawable.chip_bg)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { setMargins(0, dp(4), dp(8), dp(4)) }
        }

    private fun sectionTitle(parent: LinearLayout, text: String) {
        parent.addView(label(text, 17, navy, true).apply { setPadding(0, dp(20), 0, dp(8)) })
    }

    private fun primaryButton(text: String, click: () -> Unit): Button = button(text, Color.WHITE, R.drawable.primary_button, click)
    private fun secondaryButton(text: String, click: () -> Unit): Button = button(text, navy, R.drawable.secondary_button, click)
    private fun dangerButton(text: String, click: () -> Unit): Button = button(text, Color.WHITE, R.drawable.danger_button, click)

    private fun button(text: String, color: Int, bg: Int, click: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            isAllCaps = false
            setTextColor(color)
            background = getDrawable(bg)
            minHeight = 0
            minimumHeight = 0
            setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(10), 0, 0) }
        }

    private fun navButton(text: String, click: () -> Unit): TextView =
        centerTitle(text, 24, navy).apply {
            setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        }

    private fun navItem(icon: String, title: String, selected: Boolean, click: () -> Unit): LinearLayout =
        column(Gravity.CENTER).apply {
            setOnClickListener { click() }
            addView(centerTitle(icon, 20, if (selected) green else muted))
            addView(centerTitle(title, 11, if (selected) green else muted))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
            setTextColor(color)
            includeFontPadding = true
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun EditText.styleInput() {
        background = getDrawable(R.drawable.card_bg)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        setTextColor(Color.BLACK)
        setHintTextColor(muted)
        minHeight = dp(48)
        setPadding(dp(12), dp(10), dp(12), dp(10))
    }

    private fun small(text: String): TextView = label(text, 14, muted, false)
    private fun centerTitle(text: String, size: Int, color: Int = navy): TextView = label(text, size, color, size >= 18).apply { gravity = Gravity.CENTER }
    private fun row(gravity: Int = Gravity.NO_GRAVITY): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; this.gravity = gravity }
    private fun column(gravity: Int = Gravity.NO_GRAVITY): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; this.gravity = gravity }
    private fun matchWrap(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(8), 0, dp(8)) }
    private fun isCurrentUserWorker(): Boolean = prefs.getString("user_type", "customer") == "worker"
    private fun isAvailable(): Boolean = prefs.getBoolean("available", true)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun stars(score: Float): String = buildString { repeat(score.roundToInt().coerceIn(1, 5)) { append("★") } }
    private fun t(en: String, kn: String): String = if (lang == Lang.EN) en else kn
    private fun paymentRange(rate: String): Pair<Int, Int>? {
        val numbers = Regex("\\d+").findAll(rate).map { it.value.toIntOrNull() }.filterNotNull().toList()
        if (numbers.size < 2) return null
        val low = minOf(numbers[0], numbers[1])
        val high = maxOf(numbers[0], numbers[1])
        return low to high
    }

    private fun paymentAmount(worker: Worker): String = paymentRange(worker.rate(lang))?.first?.toString()
        ?: Regex("\\d+").find(worker.rate(lang))?.value
        ?: worker.rate(lang)

    private fun isValidPaymentAmount(amount: String): Boolean {
        val value = amount.toIntOrNull() ?: return false
        return value > 0
    }

    private fun paymentUpiId(worker: Worker): String {
        val registeredPhone = prefs.getString("worker_phone", "")
        val registeredUpiId = prefs.getString("worker_upi_id", "")?.takeIf { isValidUpiId(it) }
        return if (worker.phone == registeredPhone && registeredUpiId != null) registeredUpiId else ""
    }

    private fun isValidUpiId(value: String): Boolean =
        Regex("^[A-Za-z0-9._-]{2,}@[A-Za-z][A-Za-z0-9.-]{2,}$").matches(value.trim())

    private fun savePayment(worker: Worker, amount: String, method: String, note: String) {
        prefs.edit()
            .putString("last_payment_worker", worker.name(lang))
            .putString("last_payment_amount", amount)
            .putString("last_payment_method", method)
            .putString("last_payment_note", note)
            .apply()
    }

    private fun isUpiPaymentSuccess(data: Intent?): Boolean {
        val response = data?.getStringExtra("response") ?: data?.dataString ?: return false
        return response.split("&").any { part ->
            val pieces = part.split("=", limit = 2)
            pieces.size == 2 &&
                pieces[0].equals("Status", ignoreCase = true) &&
                pieces[1].equals("SUCCESS", ignoreCase = true)
        } || response.contains("SUCCESS", ignoreCase = true)
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(t("Logout", "ಲಾಗ್ ಔಟ್"))
            .setMessage(t("Do you want to logout and exit this session?", "ಈ ಸೆಷನ್‌ನಿಂದ ಲಾಗ್ ಔಟ್ ಆಗಬೇಕೇ?"))
            .setNegativeButton(t("Cancel", "ರದ್ದು")) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(t("Logout", "ಲಾಗ್ ಔಟ್")) { _, _ -> logout() }
            .show()
    }

    private fun logout() {
        prefs.edit()
            .putBoolean("logged_in", false)
            .remove("user_type")
            .apply()
        Toast.makeText(this, t("Logged out successfully", "ಯಶಸ್ವಿಯಾಗಿ ಲಾಗ್ ಔಟ್ ಆಗಿದೆ"), Toast.LENGTH_SHORT).show()
        showLogin()
    }

    private fun launchUpiPayment(worker: Worker, amount: String, upiId: String) {
        pendingPaymentWorkerIndex = workerList().indexOf(worker).coerceAtLeast(0)
        val upiUri = Uri.parse(
            "upi://pay?pa=${encode(upiId)}" +
                "&pn=${encode(worker.name(lang))}" +
                "&tn=${encode("Mane-Kelsa work payment")}" +
                "&am=$amount&cu=INR"
        )
        val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
        val chooser = Intent.createChooser(upiIntent, t("Choose UPI app", "UPI ಆಪ್ ಆರಿಸಿ"))
        try {
            startActivityForResult(chooser, upiPaymentRequestCode)
        } catch (_: Exception) {
            pendingPaymentWorkerIndex = -1
            Toast.makeText(this, t("No UPI app found on this phone", "ಈ ಫೋನ್‌ನಲ್ಲಿ UPI ಆಪ್ ಸಿಗಲಿಲ್ಲ"), Toast.LENGTH_LONG).show()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun goBack() {
        when (activeScreen) {
            Screen.CustomerLogin,
            Screen.WorkerLogin,
            Screen.CreateAccount,
            Screen.WorkerRegister -> showLogin()
            Screen.Login -> exitApp()
            else -> showHome()
        }
    }

    private fun exitApp() {
        finishAffinity()
    }

    private fun selectedSlotText(): String {
        val key = prefs.getString("slot", Slot.TwoHours.key)
        return Slot.values().firstOrNull { it.key == key }?.label(lang) ?: Slot.TwoHours.label(lang)
    }

    private fun redrawCurrentScreen() {
        when (activeScreen) {
            Screen.Login -> showLogin()
            Screen.CustomerLogin -> showCustomerLogin()
            Screen.WorkerLogin -> showWorkerLogin()
            Screen.CreateAccount -> showCreateAccount()
            Screen.WorkerRegister -> showWorkerRegistration()
            Screen.Home -> showHome()
            Screen.Profile -> selectedWorkerOrNull()?.let { showProfile(it) } ?: showWorkerRegistration()
            Screen.Availability -> showAvailability()
            Screen.Nearby -> showNearby()
            Screen.Call -> selectedWorkerOrNull()?.let { showCall(it) } ?: showNearby()
            Screen.Payment -> selectedWorkerOrNull()?.let { showPayment(it) } ?: showNearby()
            Screen.Rating -> selectedWorkerOrNull()?.let { showRating(it) } ?: showNearby()
        }
    }

    private fun selectedWorker(): Worker = selectedWorkerOrNull() ?: registeredWorker() ?: Worker(
        "",
        "",
        t("Worker", "ಕೆಲಸಗಾರ"),
        t("Worker", "ಕೆಲಸಗಾರ"),
        "",
        "",
        "",
        "",
        "",
        "",
        0.0,
        0.0f,
        "",
        "👤"
    )

    private fun selectedWorkerOrNull(): Worker? = workerList().getOrNull(selectedWorkerIndex)

    private fun workerList(): List<Worker> {
        val registered = registeredWorker()
        return if (registered == null) workers else listOf(registered) + workers
    }

    private fun registeredWorker(): Worker? {
        if (!prefs.getBoolean("worker_registered", false)) return null
        val name = prefs.getString("worker_name", null)?.takeIf { it.isNotBlank() } ?: return null
        val phone = prefs.getString("worker_phone", null)?.takeIf { it.isNotBlank() } ?: return null
        val address = prefs.getString("worker_address", null)?.takeIf { it.isNotBlank() } ?: return null
        val skill = prefs.getString("worker_skill", null)?.takeIf { it.isNotBlank() } ?: t("Worker", "ಕೆಲಸಗಾರ")
        val rate = prefs.getString("worker_rate", null)?.takeIf { it.isNotBlank() } ?: t("Rate not set", "ದರ ಹೊಂದಿಸಲಾಗಿಲ್ಲ")
        return Worker(name, name, skill, skill, skill, skill, rate, rate, address, address, 0.1, 5.0f, phone, "👤")
    }

    data class Worker(
        val enName: String,
        val knName: String,
        val enRole: String,
        val knRole: String,
        val enSkill: String,
        val knSkill: String,
        val enRate: String,
        val knRate: String,
        val enArea: String,
        val knArea: String,
        val distanceKm: Double,
        val rating: Float,
        val phone: String,
        val icon: String
    ) {
        fun name(lang: Lang) = if (lang == Lang.EN) enName else knName
        fun role(lang: Lang) = if (lang == Lang.EN) enRole else knRole
        fun skill(lang: Lang) = if (lang == Lang.EN) enSkill else knSkill
        fun rate(lang: Lang) = if (lang == Lang.EN) enRate else knRate
        fun area(lang: Lang) = if (lang == Lang.EN) enArea else knArea
    }

    enum class Slot(val key: String, val en: String, val kn: String) {
        TwoHours("two_hours", "2 Hours", "2 ಗಂಟೆ"),
        HalfDay("half_day", "Half Day", "ಅರ್ಧ ದಿನ"),
        FullDay("full_day", "Full Day", "ಪೂರ್ಣ ದಿನ");

        fun label(lang: Lang) = if (lang == Lang.EN) en else kn
    }

    enum class Lang { KN, EN }
    enum class Screen { Login, CustomerLogin, WorkerLogin, CreateAccount, WorkerRegister, Home, Profile, Availability, Nearby, Call, Payment, Rating }
}
