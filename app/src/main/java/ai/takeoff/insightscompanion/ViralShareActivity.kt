package ai.takeoff.insightscompanion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class ViralShareActivity : Activity() {
    private val bg = Color.rgb(6, 9, 14); private val panel = Color.rgb(14, 20, 28); private val panel2 = Color.rgb(18, 27, 37); private val border = Color.rgb(38, 52, 68); private val accent = Color.rgb(0, 228, 208); private val muted = Color.rgb(173, 185, 199); private val danger = Color.rgb(255, 102, 118)
    private data class StageUi(val value: TextView, val bar: ProgressBar)
    private val order = listOf("server_prepare" to "آماده‌سازی سرور","media_resolve" to "یافتن فایل اصلی ریلز","media_download" to "دریافت واقعی ویدیو","ai_upload" to "ارسال رسانه برای تحلیل","audio_visual_analysis" to "تحلیل صدا و تصویر","behavioral_analysis" to "تحلیل رفتاری و Retention","learning_persist" to "ثبت در حافظه یادگیری")
    private val stageViews = linkedMapOf<String, StageUi>()
    private lateinit var status: TextView; private lateinit var overallText: TextView; private lateinit var overallBar: ProgressBar; private lateinit var detailText: TextView; private lateinit var urlView: TextView; private lateinit var reportView: TextView; private lateinit var retry: Button
    private var reelUrl = ""; @Volatile private var generation = 0

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(buildUi()); consume(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); consume(intent) }
    override fun onDestroy() { generation++; super.onDestroy() }

    private fun consume(source: Intent) {
        generation++; val shared = source.getStringExtra(Intent.EXTRA_TEXT).orEmpty(); reelUrl = Regex("https?://(?:www\\.)?instagram\\.com/(?:reel|reels|p)/[A-Za-z0-9_-]+(?:/[^\\s]*)?").find(shared)?.value.orEmpty(); reportView.text = ""; retry.visibility = View.GONE; resetProgress()
        if (reelUrl.isBlank()) { urlView.text = "از Instagram روی Share بزن و «تیک‌آف» را انتخاب کن."; status.text = "لینک معتبر Instagram پیدا نشد"; status.setTextColor(danger); return }
        urlView.text = reelUrl; startAnalysis()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL; setPadding(dp(18), dp(22), dp(18), dp(30)) }
        root.addView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(18)); background = rounded(Color.rgb(6,29,31),24,Color.rgb(0,92,86)); addView(label("TAKEOFF • LIVE SERVER PROGRESS",11f,accent,true)); addView(label("کالبدشکافی زنده ریلز",27f,Color.WHITE,true).apply{setPadding(0,dp(5),0,0)}); addView(label("v${BuildConfig.VERSION_NAME} • بدون کلید • پردازش Native • Gemini",12f,muted,false).apply{setPadding(0,dp(7),0,0)}) }, lp(dp(14)))
        root.addView(card().apply { addView(label("ریلز ورودی",12f,accent,true)); urlView=label("در انتظار لینک…",13f,Color.WHITE,false).apply{setTextIsSelectable(true);setPadding(0,dp(8),0,0)}; addView(urlView) }, lp(dp(12)))
        root.addView(card().apply { addView(label("موتور سرور",12f,accent,true)); status=label("آماده…",17f,Color.WHITE,true).apply{setPadding(0,dp(8),0,dp(5))}; addView(status); overallText=label("پیشرفت کل: ۰٪",14f,accent,true); addView(overallText); overallBar=ProgressBar(this@ViralShareActivity,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=0;isIndeterminate=false}; addView(overallBar,LinearLayout.LayoutParams(-1,dp(10)).apply{topMargin=dp(6);bottomMargin=dp(8)}); detailText=label("در انتظار اولین پاسخ واقعی سرور…",12f,muted,false);addView(detailText);addView(stagePanel(),LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10)}); retry=Button(this@ViralShareActivity).apply{text="تلاش مجدد";isAllCaps=false;visibility=View.GONE;setTextColor(Color.WHITE);background=rounded(Color.rgb(29,41,54),14,Color.rgb(53,69,86));setOnClickListener{startAnalysis()}};addView(retry,LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(10)}) },lp(dp(12)))
        root.addView(card().apply { addView(label("گزارش هوشمند",12f,accent,true));addView(label("صدا، تصویر، Hook، داستان، تدوین، Retention و الگوهای رفتاری روی سرور تحلیل می‌شوند.",12f,muted,false).apply{setPadding(0,dp(6),0,dp(12))});reportView=label("",13.5f,Color.WHITE,false).apply{setLineSpacing(0f,1.22f);setTextIsSelectable(true)};addView(reportView) })
        root.addView(Button(this).apply{text="بستن";isAllCaps=false;setTextColor(Color.WHITE);background=rounded(panel2,14,border);setOnClickListener{finish()}},LinearLayout.LayoutParams(-1,dp(50)).apply{topMargin=dp(14)});scroll.addView(root);return scroll
    }

    private fun stagePanel(): View { val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=rounded(panel2,16);setPadding(dp(12),dp(10),dp(12),dp(10))}; order.forEach{(key,title)-> val h=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL};val tv=label(title,12.5f,muted,false);val vv=label("۰٪",12.5f,muted,true).apply{gravity=Gravity.END};h.addView(tv,LinearLayout.LayoutParams(0,-2,1f));h.addView(vv,LinearLayout.LayoutParams(dp(56),-2));box.addView(h);val b=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=100;progress=0;isIndeterminate=false};box.addView(b,LinearLayout.LayoutParams(-1,dp(6)).apply{topMargin=dp(3);bottomMargin=dp(7)});stageViews[key]=StageUi(vv,b)};return box }

    private fun startAnalysis() { if(reelUrl.isBlank())return;val mine=++generation;retry.visibility=View.GONE;reportView.text="";resetProgress();status.text="در حال اتصال مستقیم به موتور Native…";status.setTextColor(Color.WHITE);val niche=getSharedPreferences("takeoff_companion_plain",Context.MODE_PRIVATE).getString("viral_analysis_niche","عمومی").orEmpty().ifBlank{"عمومی"};Thread{var terminal=false;val result=runCatching{ViralStreamClient.analyze(reelUrl,niche){e->if(generation!=mine)return@analyze;if(e.optString("type")=="error"||e.optString("stage")=="completed")terminal=true;runOnUiThread{if(generation==mine)renderEvent(e)}}}.getOrElse{if(generation==mine)runOnUiThread{showError("ارتباط Stream با سرور قطع شد",it.javaClass.simpleName)};return@Thread};if(generation!=mine)return@Thread;if(result.httpCode !in 200..299)runOnUiThread{showError("سرور پاسخ معتبر نداد","HTTP ${result.httpCode}")}else if(!terminal)runOnUiThread{showError("Stream زودتر از پایان بسته شد","server_stream_incomplete")}}.start() }

    private fun renderEvent(root:JSONObject){if(root.optString("type")=="error"){showError("پردازش روی سرور متوقف شد",root.optString("error_code","server_processing_failed"));return};val stage=root.optString("stage");val sp=root.optInt("stage_progress_percent",0).coerceIn(0,100);val op=root.optInt("overall_progress_percent",0).coerceIn(0,100);val idx=order.indexOfFirst{it.first==stage};if(idx>=0){order.take(idx).forEach{setStage(it.first,100,true)};setStage(stage,sp,sp>=100)}else if(stage=="completed")order.forEach{setStage(it.first,100,true)};overallBar.progress=op;overallText.text="پیشرفت کل: ${fa(op)}٪ • مرحله جاری: ${fa(sp)}٪";val sl=root.optString("stage_label").ifBlank{stage};status.text=if(stage=="completed")"تحلیل کامل شد" else "$sl — ${fa(sp)}٪";status.setTextColor(if(stage=="completed")accent else Color.WHITE);val elapsed=root.optLong("elapsed_ms",0);val dl=root.optLong("downloaded_bytes",0);val ul=root.optLong("uploaded_bytes",0);val total=root.optLong("total_bytes",0);detailText.text=when{dl>0->"دریافت: ${bytes(dl)}${if(total>0)" از ${bytes(total)}" else ""}${elapsedText(elapsed)}";ul>0->"ارسال برای تحلیل: ${bytes(ul)}${if(total>0)" از ${bytes(total)}" else ""}${elapsedText(elapsed)}";root.has("model")->"مدل فعال: ${root.optString("model")}${elapsedText(elapsed)}";elapsed>0->"زمان سپری‌شده: ${formatSeconds(elapsed)}";else->"پیشرفت از خود سرور دریافت می‌شود؛ درصد ساختگی نمایش داده نمی‌شود."};if(stage=="completed")showCompleted(root.optJSONObject("result")?:JSONObject())}
    private fun setStage(k:String,pct:Int,done:Boolean){val u=stageViews[k]?:return;val p=pct.coerceIn(0,100);u.bar.progress=p;u.value.text=(if(done)"✓ " else "")+fa(p)+"٪";u.value.setTextColor(if(done)accent else if(p>0)Color.WHITE else muted)}
    private fun showCompleted(result:JSONObject){overallBar.progress=100;overallText.text="پیشرفت کل: ۱۰۰٪ • همه مراحل تکمیل شد";retry.visibility=View.GONE;val r=result.optJSONObject("report")?:JSONObject();reportView.text=buildString{append("خلاصه\n").append(r.optString("summary","—")).append("\n\n");r.optString("dialogue").takeIf{it.isNotBlank()}?.let{append("دیالوگ تشخیص‌داده‌شده\n").append(it).append("\n\n")};for((t,k) in listOf("Hook" to "hook","داستان و Payoff" to "story","تدوین" to "editing","صدا و تصویر" to "audio_visual","Retention" to "retention","الگوهای رفتاری" to "behavioral_patterns","الگوهای قابل استفاده" to "reusable_patterns","پیشنهادها" to "recommendations")){append(t).append("\n").append(pretty(r.opt(k))).append("\n\n")}}}
    private fun pretty(v:Any?):String=when(v){null->"—";is JSONObject->v.toString(2);else->v.toString()};private fun showError(t:String,d:String=""){status.text=t;status.setTextColor(danger);detailText.text=if(d.isBlank())"جزئیات امنی برای نمایش نیست." else "کد: $d";retry.visibility=View.VISIBLE};private fun resetProgress(){if(::overallBar.isInitialized)overallBar.progress=0;if(::overallText.isInitialized)overallText.text="پیشرفت کل: ۰٪";if(::detailText.isInitialized)detailText.text="در انتظار اولین پاسخ واقعی سرور…";stageViews.keys.forEach{setStage(it,0,false)}}
    private fun elapsedText(ms:Long)=if(ms>0)" • ${formatSeconds(ms)}" else "";private fun formatSeconds(ms:Long)="${fa((ms/1000).toInt())} ثانیه";private fun bytes(v:Long):String{val mb=v/(1024.0*1024.0);return if(mb>=1)String.format(java.util.Locale.US,"%.1f MB",mb) else "${v/1024} KB"};private fun fa(v:Int)=v.toString().map{when(it){'0'->'۰';'1'->'۱';'2'->'۲';'3'->'۳';'4'->'۴';'5'->'۵';'6'->'۶';'7'->'۷';'8'->'۸';'9'->'۹';else->it}}.joinToString("")
    private fun card()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(18),dp(16),dp(18),dp(16));background=rounded(panel,20,border)};private fun label(t:String,s:Float,c:Int,b:Boolean)=TextView(this).apply{text=t;textSize=s;setTextColor(c);typeface=if(b)Typeface.DEFAULT_BOLD else Typeface.DEFAULT;layoutDirection=View.LAYOUT_DIRECTION_RTL;textDirection=View.TEXT_DIRECTION_RTL};private fun rounded(c:Int,r:Int,stroke:Int?=null)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();stroke?.let{setStroke(dp(1),it)}};private fun lp(b:Int)=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=b};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
