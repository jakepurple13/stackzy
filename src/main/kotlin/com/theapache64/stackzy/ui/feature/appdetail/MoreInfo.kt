package com.theapache64.stackzy.ui.feature.appdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theapache64.stackzy.data.local.GradleInfo
import com.theapache64.stackzy.model.AnalysisReportWrapper
import com.theapache64.stackzy.ui.common.GradientMargin
import com.theapache64.stackzy.ui.theme.R
import kotlin.text.Regex.Companion.fromLiteral


private val firaCode by lazy {
    FontFamily(
        Font("fonts/FiraCode-Regular.ttf")
    )
}


private val codeViewerPadding = 10.dp
private val codeViewerTitleContentSpacerHeight = 4.dp
private val codeViewerFontSize = 14.sp

@Composable
fun MoreInfo(
    report: AnalysisReportWrapper,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            BuildGradleGroovy(
                packageName = report.packageName,
                gradleInfo = report.gradleInfo
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        if (report.permissions.isNotEmpty()) {
            item {
                PermissionsXml(
                    permission = report.permissions
                )
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        if (report.fullManifest.isNotEmpty()) {
            item {
                FullManifest(manifest = report.fullManifest)
            }
        }

        item {
            GradientMargin()
        }
    }
}

@Composable
private fun BuildGradleGroovy(
    modifier: Modifier = Modifier,
    packageName: String,
    gradleInfo: GradleInfo
) {
    Column(
        modifier = modifier.padding(top = 10.dp)
    ) {

        // Title
        Text(
            text = "build.gradle",
            style = MaterialTheme.typography.body2
        )

        Spacer(
            modifier = Modifier.height(codeViewerTitleContentSpacerHeight)
        )

        Text(
            modifier = Modifier
                .background(MaterialTheme.colors.secondary, RoundedCornerShape(5.dp))
                .padding(codeViewerPadding),
            text = with(AnnotatedString.Builder("")) {

                // line #1
                pushStyle(SpanStyle(R.color.JordyBlue))
                append("android")
                pushStyle(SpanStyle(Color.White))
                append(" {\n")

                // line #2
                pushStyle(SpanStyle(R.color.JordyBlue))
                append("  defaultConfig")
                pushStyle(SpanStyle(Color.White))
                append(" {\n")

                // line #3
                pushStyle(SpanStyle(R.color.JordyBlue))
                append("    applicationId")
                pushStyle(SpanStyle(R.color.YellowGreen))
                append(" \"${packageName}\"")

                gradleInfo.minSdk?.let { (minSdkInt, minSdkVersionName) ->
                    // line #4
                    pushStyle(SpanStyle(R.color.JordyBlue))
                    append("\n    minSdkVersion")
                    pushStyle(SpanStyle(R.color.WildWatermelon))
                    append(" $minSdkInt")

                    // sdk version as comment
                    pushStyle(SpanStyle(R.color.BlueBayoux))
                    append(" // $minSdkVersionName")
                }

                gradleInfo.targetSdk?.let { (targetSdkInt, targetSdkVersionName) ->
                    // line #5
                    pushStyle(SpanStyle(R.color.JordyBlue))
                    append("\n    targetSdkVersion")
                    pushStyle(SpanStyle(R.color.WildWatermelon))
                    append(" $targetSdkInt")

                    // sdk version as comment
                    pushStyle(SpanStyle(R.color.BlueBayoux))
                    append(" // $targetSdkVersionName")
                }

                gradleInfo.versionCode?.let { versionCode ->
                    // line #6
                    pushStyle(SpanStyle(R.color.JordyBlue))
                    append("\n    versionCode")
                    pushStyle(SpanStyle(R.color.WildWatermelon))
                    append(" $versionCode")
                }


                gradleInfo.versionName?.let { versionName ->
                    // line #6
                    pushStyle(SpanStyle(R.color.JordyBlue))
                    append("\n    versionName")
                    pushStyle(SpanStyle(R.color.YellowGreen))
                    append(" \"$versionName\"")
                }

                pushStyle(SpanStyle(Color.White))
                append("\n  }\n")
                append("}")

                toAnnotatedString()
            },
            fontFamily = firaCode,
            fontSize = codeViewerFontSize
        )
    }
}

@Composable
private fun PermissionsXml(
    modifier: Modifier = Modifier,
    permission: Set<String>
) {
    Column(modifier) {

        // Title
        Text(
            text = "AndroidManifest.xml",
            style = MaterialTheme.typography.body2
        )

        Spacer(
            modifier = Modifier.height(codeViewerTitleContentSpacerHeight)
        )

        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.secondary, RoundedCornerShape(5.dp))
                .padding(codeViewerPadding)
        ) {

            // Permissions
            permission.forEach { permission ->
                Text(
                    text = with(AnnotatedString.Builder("")) {
                        pushStyle(SpanStyle(color = R.color.WildWatermelon))
                        append("<uses-permission ")
                        pushStyle(SpanStyle(color = R.color.Goldenrod))
                        append("android:name")
                        pushStyle(SpanStyle(color = Color.White))
                        append("=")
                        pushStyle(SpanStyle(color = R.color.YellowGreen))
                        append("\"$permission\"")
                        pushStyle(SpanStyle(color = R.color.WildWatermelon))
                        append("/>")
                        toAnnotatedString()
                    },
                    fontFamily = firaCode,
                    fontSize = codeViewerFontSize
                )
            }
        }
    }
}

@Composable
private fun FullManifest(
    modifier: Modifier = Modifier,
    manifest: String
) {
    Column(modifier) {

        // Title
        Text(
            text = "AndroidManifest.xml",
            style = MaterialTheme.typography.body2
        )

        Spacer(
            modifier = Modifier.height(codeViewerTitleContentSpacerHeight)
        )

        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.secondary, RoundedCornerShape(5.dp))
                .padding(codeViewerPadding)
        ) {
            SelectionContainer {
                Text(
                    text = codeString(manifest),
                    fontFamily = firaCode,
                    fontSize = codeViewerFontSize
                )
            }
        }
    }
}

class Code(
    val simple: SpanStyle = SpanStyle(Color(0xFFA9B7C6)),
    val value: SpanStyle = SpanStyle(Color(0xFF6897BB)),
    val keyword: SpanStyle = SpanStyle(Color(0xFFCC7832)),
    val punctuation: SpanStyle = SpanStyle(Color(0xFFA1C17E)),
    val annotation: SpanStyle = SpanStyle(Color(0xFFBBB529)),
    val comment: SpanStyle = SpanStyle(Color(0xFF808080))
)

private fun codeString(str: String) = buildAnnotatedString {
    val code = Code()
    withStyle(code.simple) {
        append(str)
        addStyle(SpanStyle(R.color.WildWatermelon), str, Regex("<(.*?) "))
        addStyle(SpanStyle(R.color.WildWatermelon), str, Regex("<\\?(.*?) "))
        addStyle(SpanStyle(R.color.WildWatermelon), str, Regex("<(.*?)>"))
        addStyle(SpanStyle(R.color.Goldenrod), str, Regex("(.*?)="))
        addStyle(SpanStyle(R.color.Goldenrod), str, Regex("android:(.*?)="))
        addStyle(SpanStyle(R.color.Goldenrod), str, Regex("xmlns:(.*?)="))
        addStyle(SpanStyle(Color.White), str, Regex("="))
        addStyle(SpanStyle(R.color.YellowGreen), str, Regex("\"(.*?)\""))
        addStyle(SpanStyle(R.color.WildWatermelon), str, Regex("/>"))
        addStyle(SpanStyle(R.color.WildWatermelon), str, Regex(">"))
        addStyle(SpanStyle(R.color.WildWatermelon), str, Regex("</(.*?)>"))
    }
}

private fun AnnotatedString.Builder.addStyle(style: SpanStyle, text: String, regexp: String) {
    addStyle(style, text, fromLiteral(regexp))
}

private fun AnnotatedString.Builder.addStyle(style: SpanStyle, text: String, regexp: Regex) {
    for (result in regexp.findAll(text)) {
        addStyle(style, result.range.first, result.range.last + 1)
    }
}