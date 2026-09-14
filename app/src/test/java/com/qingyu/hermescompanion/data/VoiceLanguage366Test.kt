package com.qingyu.hermescompanion.data

import com.qingyu.hermescompanion.model.*
import com.qingyu.hermescompanion.storage.SecureCookieJar
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock

class VoiceLanguage366Test {
    @Test fun savingEnglishAndAutomaticDetectionKeepsProviderAndUnrelatedSettings() {
        val server=MockWebServer()
        server.start()
        val client=HermesApiClient(ConnectionConfig(server.url("/").toString().trimEnd('/'),"test"),mock(SecureCookieJar::class.java))
        client.setProfile("english")
        try {
            for (language in listOf("en", "")) {
                server.enqueue(MockResponse().setBody("""{"config":{"stt":{"provider":"local","local":{"language":"zh","model":"small"}},"agent":{"max_turns":99},"tts":{"provider":"edge","edge":{"voice":"en-US-JennyNeural"}}}}"""))
                server.enqueue(MockResponse().setBody("{\"ok\":true}"))
                val result=client.saveVoiceSettings(ServerVoiceSettings(stt=ServerSttSettings(provider="local",model="small",language=language),tts=ServerTtsSettings(provider="edge",voice="en-US-JennyNeural")))
                server.takeRequest()
                val request=server.takeRequest()
                assertEquals("PUT", request.method)
                assertEquals("english",request.requestUrl!!.queryParameter("profile"))
                val config=JSONObject(request.body.readUtf8()).getJSONObject("config")
                assertEquals(language,config.getJSONObject("stt").getJSONObject("local").getString("language"))
                assertEquals("small",config.getJSONObject("stt").getJSONObject("local").getString("model"))
                assertEquals(99,config.getJSONObject("agent").getInt("max_turns"))
                assertEquals(language,result.voice.stt.language)
            }
        } finally { client.close();server.shutdown() }
    }
}
