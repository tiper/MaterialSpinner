package com.tiper.materialspinner.sample

import android.support.annotation.IdRes
import android.support.test.espresso.DataInteraction
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.*
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun popupTest() {

        onView(withId(R.id.material_spinner_2)).perform(click())

        onPopup().perform(click())

        onSpinner(R.id.material_spinner_2).check(matches(withText("Mercury")))
    }

    @Test
    fun errorLayoutTest() {

        onView(withId(R.id.b1_error)).perform(click())

        onErrorLayout(R.id.material_spinner_1).check(matches(isDisplayed()))

        onView(withId(R.id.b1_error)).perform(click())

        onErrorLayout(R.id.material_spinner_1).check(matches(not(isDisplayed())))
    }

    @Test
    fun clearTest() {

        onView(withId(R.id.material_spinner_2)).perform(click())

        onPopup().perform(click())

        onSpinner(R.id.material_spinner_2).check(matches(withText("Mercury")))

        onView(withId(R.id.b2_clear)).perform(click())

        onSpinner(R.id.material_spinner_2).check(matches(withText(String())))
    }

    private fun atPosition(position: Int, parentMatcher: Matcher<View>): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendDescriptionOf(parentMatcher)
            }

            public override fun matchesSafely(view: View): Boolean {
                return view.parent.let {
                    it is ViewGroup && parentMatcher.matches(it) && view == it.getChildAt(position)
                }
            }
        }
    }

    private fun onErrorLayout(@IdRes id: Int): ViewInteraction {
        return onView(
            allOf(
                withId(R.id.textinput_error),
                atPosition(0, atPosition(0, withParent(withId(id))))
            )
        )
    }

    private fun onSpinner(@IdRes id: Int): ViewInteraction {
        return onView(atPosition(0, atPosition(0, withId(id))))
    }

    private fun onPopup(): DataInteraction {
        return onData(anything())
            .inAdapterView(
                atPosition(
                    0,
                    withClassName(`is`("android.widget.PopupWindow\$PopupBackgroundView"))
                )
            ).atPosition(0)
    }

}
