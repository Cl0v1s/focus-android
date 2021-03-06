/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.OneShotOnPreDrawListener

class SessionsSheetFragment : LocaleAwareFragment(), View.OnClickListener {

    private lateinit var backgroundView: View
    private lateinit var cardView: View
    private var isAnimating: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sessionssheet, container, false)

        backgroundView = view.findViewById(R.id.background)
        backgroundView.setOnClickListener(this)

        cardView = view.findViewById(R.id.card)
        OneShotOnPreDrawListener(cardView) {
            playAnimation(false)
            true
        }

        val sessionManager = requireComponents.sessionManager

        val sessionsAdapter = SessionsAdapter(this, sessionManager.sessions)
        sessionManager.register(sessionsAdapter, owner = this)

        view.findViewById<RecyclerView>(R.id.sessions).let {
            it.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            it.adapter = sessionsAdapter
        }

        return view
    }

    @Suppress("ComplexMethod")
    private fun playAnimation(reverse: Boolean): Animator {
        isAnimating = true

        val sheetAnimator = ValueAnimator.ofFloat(if (reverse) 1f else 0f, if (reverse) 0f else 1f);
        sheetAnimator.apply {
            duration = ANIMATION_DURATION.toLong()
            interpolator = AccelerateInterpolator()
            addUpdateListener(AnimatorUpdateListener {
                cardView.scaleX = it.animatedValue as Float
                cardView.scaleY = it.animatedValue as Float
                cardView.alpha = it.animatedValue as Float
            })
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    cardView.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false

                    cardView.visibility = if (reverse) View.GONE else View.VISIBLE
                }
            })
            start()
        }


        backgroundView.alpha = if (reverse) 1f else 0f
        backgroundView.animate()
            .alpha(if (reverse) 0f else 1f)
            .setDuration(ANIMATION_DURATION.toLong())
            .start()

        return sheetAnimator
    }

    internal fun animateAndDismiss(): Animator {
        val animator = playAnimation(true)

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val activity = activity as MainActivity?
                activity?.supportFragmentManager?.beginTransaction()?.remove(this@SessionsSheetFragment)?.commit()
            }
        })

        return animator
    }

    fun onBackPressed(): Boolean {
        animateAndDismiss()

        TelemetryWrapper.closeTabsTrayEvent()
        return true
    }

    override fun applyLocale() {}

    override fun onClick(view: View) {
        if (isAnimating) {
            // Ignore touched while we are animating
            return
        }

        when (view.id) {
            R.id.background -> {
                animateAndDismiss()

                TelemetryWrapper.closeTabsTrayEvent()
            }

            else -> throw IllegalStateException("Unhandled view in onClick()")
        }
    }

    companion object {
        const val FRAGMENT_TAG = "tab_sheet"

        private const val ANIMATION_DURATION = 200
    }
}
