package com.danielcrvo.sintonia.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XtreamTest {
    private val creds = XtreamCredentials("http://servidor.exemplo:8080", "ana", "s3nh@ #1")

    @Test
    fun `normalizes the server`() {
        assertEquals("http://servidor.exemplo:8080", Xtream.normalizeServer("servidor.exemplo:8080"))
        assertEquals("https://s.exemplo", Xtream.normalizeServer("HTTPS://s.exemplo/"))
        assertEquals("http://s.exemplo", Xtream.normalizeServer("http://s.exemplo/player_api.php?username=a"))
        assertEquals("http://s.exemplo/xtream", Xtream.normalizeServer("  s.exemplo/xtream/  "))
    }

    @Test
    fun `builds escaped URLs`() {
        assertEquals(
            "http://servidor.exemplo:8080/player_api.php?username=ana&password=s3nh%40%20%231&action=get_live_streams",
            Xtream.apiUrl(creds, "get_live_streams"),
        )
        assertEquals(
            "http://servidor.exemplo:8080/live/ana/s3nh%40%20%231/101.m3u8",
            Xtream.playbackUrl(creds, "live", "101", "m3u8"),
        )
    }

    @Test
    fun `host without scheme, port and path`() {
        assertEquals("servidor.exemplo", hostOf("http://usuario:senha@servidor.exemplo:8080/x?y=1"))
        assertNull(hostOf("  "))
    }

    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `maps a channel with numbers as text or number`() {
        val channel = XtreamMapper.channel(
            obj("""{"stream_id":101,"num":"7","name":" Rede Horizonte ","stream_icon":"","category_id":"3","epg_channel_id":"rede.h"}"""),
        )!!
        assertEquals("101", channel.externalId)
        assertEquals(7, channel.number)
        assertEquals("Rede Horizonte", channel.name)
        assertNull(channel.logo)
        assertEquals("rede.h", channel.epgId)
        assertNull(XtreamMapper.channel(obj("""{"stream_id":1}""")))
    }

    @Test
    fun `movie uses rating directly, then the 5-star score, and year from the date`() {
        val direct = XtreamMapper.movie(obj("""{"stream_id":1,"name":"Eco","rating":"7.5","releasedate":"2021-03-04"}"""))!!
        assertEquals(7.5, direct.rating)
        assertEquals("2021", direct.year)
        val five = XtreamMapper.movie(obj("""{"stream_id":2,"name":"Brisa","rating":"0","rating_5based":3.5,"year":"2019"}"""))!!
        assertEquals(7.0, five.rating)
        assertEquals("2019", five.year)
        assertNull(XtreamMapper.movie(obj("""{"stream_id":3,"name":"Sem nota","rating":"0"}"""))!!.rating)
    }

    @Test
    fun `category requires id and name`() {
        assertEquals("Notícias", XtreamMapper.category(obj("""{"category_id":"1","category_name":"Notícias"}"""))!!.name)
        assertNull(XtreamMapper.category(obj("""{"category_id":"1"}""")))
    }

    @Test
    fun `movie details`() {
        val info = XtreamMapper.vodInfo(
            obj(
                """{"info":{"plot":"Uma viagem.","actors":"Ana, Beto","genre":"Aventura","duration_secs":"5400","backdrop_path":["http://img/b.jpg"]},"movie_data":{"container_extension":"mkv"}}""",
            ),
        )
        assertEquals("Uma viagem.", info.plot)
        assertEquals("Ana, Beto", info.cast)
        assertEquals(5400, info.durationSecs)
        assertEquals("http://img/b.jpg", info.backdropUrl)
        assertEquals("mkv", info.containerExtension)
    }

    @Test
    fun `series details with seasons as an object, sorted`() {
        val info = XtreamMapper.seriesInfo(
            obj(
                """{"info":{"plot":"Drama","cover":"http://img/c.jpg"},"episodes":{
                "2":[{"id":"22","episode_num":"1","title":"Volta","container_extension":"mp4","info":{"duration_secs":2700}}],
                "1":[{"id":"12","episode_num":2,"title":"Meio"},{"id":"11","episode_num":1,"title":"Começo","info":{"movie_image":"http://img/e.jpg"}},{"episode_num":3}]}}""",
            ),
        )
        assertEquals(listOf(1, 2), info.seasons.map { it.number })
        assertEquals(listOf("11", "12"), info.seasons[0].episodes.map { it.externalId })
        assertEquals("http://img/e.jpg", info.seasons[0].episodes[0].imageUrl)
        assertEquals(2700, info.seasons[1].episodes[0].durationSecs)
        assertEquals("http://img/c.jpg", info.backdropUrl)
        assertEquals(3, info.episodeCount)
    }

    @Test
    fun `seasons as a list of lists`() {
        val info = XtreamMapper.seriesInfo(obj("""{"episodes":[[{"id":"1","episode_num":1,"title":"A"}],[{"id":"2","episode_num":1,"title":"B"}]]}"""))
        assertEquals(listOf(1, 2), info.seasons.map { it.number })
    }
}
