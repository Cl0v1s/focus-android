/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.session.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.session.Session
import org.mozilla.focus.R
import org.mozilla.focus.ext.beautifyUrl
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.telemetry.TelemetryWrapper
import java.lang.ref.WeakReference

class SessionViewHolder internal constructor(
    private val fragment: SessionsSheetFragment,
    private val textView: TextView
) : RecyclerView.ViewHolder(textView), View.OnTouchListener {
    companion object {
        @JvmField
        internal val LAYOUT_ID = R.layout.item_session
    }

    private var sessionReference: WeakReference<Session> = WeakReference<Session>(null)
    private var startX: Float = 0f
    private var startY: Float = 0f

    init {
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_link, 0, 0, 0)
        textView.setOnTouchListener(this)
    }

    fun bind(session: Session) {
        this.sessionReference = WeakReference(session)

        updateTitle(session)

        val isCurrentSession = fragment.requireComponents.sessionManager.selectedSession == session

        updateTextBackgroundColor(isCurrentSession)
    }

    private fun updateTextBackgroundColor(isCurrentSession: Boolean) {
        val drawable = if (isCurrentSession) {
            R.drawable.background_list_item_current_session
        } else {
            R.drawable.background_list_item_session
        }
        textView.setBackgroundResource(drawable)
    }

    private fun updateTitle(session: Session) {
        textView.text =
            if (session.title.isEmpty()) session.url.beautifyUrl()
            else session.title
    }

    fun onClick() {
        val session = sessionReference.get() ?: return
        selectSession(session)
    }

    private fun selectSession(session: Session) {
        fragment.animateAndDismiss().addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                fragment.requireComponents.sessionManager.select(session)

                TelemetryWrapper.switchTabInTabsTrayEvent()
            }
        })
    }

    private fun removeSession(session: Session) {
        fragment.requireComponents.sessionManager.remove(session)
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY
        val disX = (rawX - startX)
        val disY = (rawY - startY)
        val distance = Math.sqrt((disX * disX + disY * disY).toDouble()).toInt()
        when(event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                startX = rawX
                startY = rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if(distance > 0 && disX > 0) {
                    textView.x = distance.toFloat()
                    if(disX > textView.width / 2) {
                        val session = sessionReference.get()
                        removeSession(session!!)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if(distance == 0) onClick()
                textView.animate()
                        .setInterpolator(DecelerateInterpolator())
                        .setDuration(300)
                        .xBy(-textView.x)
                        .start()
            }
            MotionEvent.ACTION_CANCEL -> {
                textView.animate()
                        .setInterpolator(DecelerateInterpolator())
                        .setDuration(300)
                        .xBy(-textView.x)
                        .start()
                return true
            }
            else -> return false
        }
        return true
    }

}
