package clearchat

import configs.Conf
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import pluginloader.api.*
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.random.Random

interface ClearChat{
    fun replace(player: Player, message: String, filter: Boolean): String?
}

fun Plugin.clearChat(): ClearChat{
    val impl = ClearChatImpl()
    onlinePlayers.forEach{impl.latests[it.uuid] = ArrayList(2)}
    impls.add(impl)
    unloadHandler{impls.remove(impl)}
    return impl
}

private val impls = ArrayList<ClearChatImpl>()

@Conf
internal var config = Config()

private class ClearChatImpl: ClearChat{
    val latests = HashMap<UUID, MutableList<String>>()

    override fun replace(player: Player, message: String, filter: Boolean): String? {
        return replace(player, message, latests[player.uuid], filter)
    }
}

@Load
internal fun load(){
    Bukkit.getOnlinePlayers().forEach{latests[it.uuid] = ArrayList(2)}
}

internal val latests = HashMap<UUID, MutableList<String>>()

@Listener
internal fun join(event: PlayerJoinEvent){
    latests[event.player.uuid] = ArrayList(2)
    impls.forEach{it.latests[event.player.uuid] = ArrayList(2)}
}

@Listener
internal fun leave(event: PlayerQuitEvent){
    latests.remove(event.player.uuid)
    impls.forEach{it.latests.remove(event.player.uuid)}
}

@Listener(priority = EventPriority.LOW)
internal fun chat(event: AsyncPlayerChatEvent){
    if(event.isCancelled)return
    val latest = latests[event.player.uuid]
    val msg = replace(event.player, event.message, latest, true)
    if(msg == null){
        event.message = "!"
        event.cancel()
        return
    }
    event.message = msg
}

private fun replace(player: Player, sourceMsg: String, latest: MutableList<String>?, filter: Boolean): String?{
    var message = sourceMsg
    if(latest != null) {
        if (latest.size < 2){
            latest.add(message)
        }else{
            var contains = true
            latest.forEach{if(it != message)contains = false}
            latest.removeAt(0)
            latest.add(message)
            if(contains){
                player.sendMessage(config.flood)
                return null
            }
        }
        if(message.length > 10 && latest.size == 2) {
            var contains = true
            latest.forEach{if (it != message) contains = false}
            if (contains) {
                player.sendMessage(config.flood)
                return null
            }
        }
    }
    if(filter) {
        message = message.split(" ").stream().map {
            if (config.list.contains(it.lowercase().replace("!", "").replace(",", "").replace(".", ""))) {
                val random = config.replaces[Random.nextInt(config.replaces.size)]
                if (it.startsWith("!")) "!$random"
                else random
            } else it
        }.collect(Collectors.joining(" "))
    }
    val buf = StringBuffer(message.length)
    var last: Char = 0.toChar()
    var lastCount = 1
    message.toCharArray().forEach{
        if(it == last){
            lastCount++
            if(lastCount > 3) return@forEach
        }else lastCount = 1
        last = it
        buf.append(it)
    }
    val msg = buf.toString()
    val lower = msg.lowercase()
    var caps = 0
    val msgChars = msg.toCharArray()
    lower.toCharArray().forEachIndexed{index, c -> if(msgChars[index] != c)caps++}
    return if(msg.length > 4 && caps > msg.length / 2) lower else msg
}

@Serializable
internal class Config(
    val flood: String = "§8[§ci§8]§f Не надо засорять чат, подумай об игроках",
    val replaces: List<String> = listOf("^-^", "^_^", "T_T", "o_O", "o_o"),
    val list: Set<String> = setOf(
        "фыв",
        "6ля",
        "6лядь",
        "6лять",
        "b3ъeб",
        "cock",
        "cunt",
        "e6aль",
        "ebal",
        "eblan",
        "eбaл",
        "eбaть",
        "eбyч",
        "eбать",
        "eбёт",
        "eблантий",
        "fuck",
        "fucker",
        "fucking",
        "xyёв",
        "xyй",
        "xyя",
        "xуе",
        "xуй",
        "xую",
        "zaeb",
        "zaebal",
        "zaebali",
        "zaebat",
        "архипиздрит",
        "ахуел",
        "ахуеть",
        "бздение",
        "бздеть",
        "бздех",
        "бздецы",
        "бздит",
        "бздицы",
        "бздло",
        "бзднуть",
        "бздун",
        "бздунья",
        "бздюха",
        "бздюшка",
        "бздюшко",
        "бля",
        "блябу",
        "блябуду",
        "бляд",
        "бляди",
        "блядина",
        "блядище",
        "блядки",
        "блядовать",
        "блядство",
        "блядун",
        "блядуны",
        "блядунья",
        "блядь",
        "блядюга",
        "блять",
        "вафел",
        "вафлёр",
        "взъебка",
        "взьебка",
        "взьебывать",
        "въеб",
        "въебался",
        "въебенн",
        "въебусь",
        "въебывать",
        "выблядок",
        "выблядыш",
        "выеб",
        "выебать",
        "выебен",
        "выебнулся",
        "выебон",
        "выебываться",
        "выпердеть",
        "высраться",
        "выссаться",
        "вьебен",
        "гавно",
        "гавнюк",
        "гавнючка",
        "гамно",
        "гандон",
        "гнид",
        "гнида",
        "гниды",
        "говенка",
        "говенный",
        "говешка",
        "говназия",
        "говнецо",
        "говнище",
        "говно",
        "говноед",
        "говнолинк",
        "говночист",
        "говнюк",
        "говнюха",
        "говнядина",
        "говняк",
        "говняный",
        "говнять",
        "гондон",
        "доебываться",
        "долбоеб",
        "долбоёб",
        "долбоящер",
        "дрисня",
        "дрист",
        "дристануть",
        "дристать",
        "дристун",
        "дристуха",
        "дрочелло",
        "дрочена",
        "дрочила",
        "дрочилка",
        "дрочистый",
        "дрочить",
        "дрочка",
        "дрочун",
        "е6ал",
        "е6ут",
        "еб твою мать",
        "ёб твою мать",
        "ёбaн",
        "ебaть",
        "ебyч",
        "ебал",
        "ебало",
        "ебальник",
        "ебан",
        "ебанамать",
        "ебанат",
        "ебаная",
        "ёбаная",
        "ебанический",
        "ебанный",
        "ебанныйврот",
        "ебаное",
        "ебануть",
        "ебануться",
        "ёбаную",
        "ебаный",
        "ебанько",
        "ебарь",
        "ебат",
        "ёбат",
        "ебатория",
        "ебать",
        "ебать-копать",
        "ебаться",
        "ебашить",
        "ебёна",
        "ебет",
        "ебёт",
        "ебец",
        "ебик",
        "ебин",
        "ебись",
        "ебическая",
        "ебки",
        "ебла",
        "еблан",
        "ебливый",
        "еблище",
        "ебло",
        "еблыст",
        "ебля",
        "ёбн",
        "ебнуть",
        "ебнуться",
        "ебня",
        "ебошить",
        "ебская",
        "ебский",
        "ебтвоюмать",
        "ебун",
        "ебут",
        "ебуч",
        "ебуче",
        "ебучее",
        "ебучий",
        "ебучим",
        "ебущ",
        "ебырь",
        "елда",
        "елдак",
        "елдачить",
        "жопа",
        "жопу",
        "заговнять",
        "задрачивать",
        "задристать",
        "задрота",
        "зае6",
        "заё6",
        "заеб",
        "заёб",
        "заеба",
        "заебал",
        "заебанец",
        "заебастая",
        "заебастый",
        "заебать",
        "заебаться",
        "заебашить",
        "заебистое",
        "заёбистое",
        "заебистые",
        "заёбистые",
        "заебистый",
        "заёбистый",
        "заебись",
        "заебошить",
        "заебываться",
        "залуп",
        "залупа",
        "залупаться",
        "залупить",
        "залупиться",
        "замудохаться",
        "запиздячить",
        "засерать",
        "засерун",
        "засеря",
        "засирать",
        "засрун",
        "захуячить",
        "заябестая",
        "злоеб",
        "злоебучая",
        "злоебучее",
        "злоебучий",
        "ибанамат",
        "ибонех",
        "изговнять",
        "изговняться",
        "изъебнуться",
        "ипать",
        "ипаться",
        "ипаццо",
        "Какдвапальцаобоссать",
        "конча",
        "курва",
        "курвятник",
        "лох",
        "лошарa",
        "лошара",
        "лошары",
        "лошок",
        "лярва",
        "малафья",
        "манда",
        "мандавошек",
        "мандавошка",
        "мандавошки",
        "мандей",
        "мандень",
        "мандеть",
        "мандища",
        "мандой",
        "манду",
        "мандюк",
        "минет",
        "минетчик",
        "минетчица",
        "млять",
        "мокрощелка",
        "мокрощёлка",
        "мразь",
        "мудak",
        "мудaк",
        "мудаг",
        "мудак",
        "муде",
        "мудель",
        "мудеть",
        "муди",
        "мудил",
        "мудила",
        "мудистый",
        "мудня",
        "мудоеб",
        "мудозвон",
        "мудоклюй",
        "на хер",
        "на хуй",
        "набздел",
        "набздеть",
        "наговнять",
        "надристать",
        "надрочить",
        "наебать",
        "наебет",
        "наебнуть",
        "наебнуться",
        "наебывать",
        "напиздел",
        "напиздели",
        "напиздело",
        "напиздили",
        "насрать",
        "настопиздить",
        "нахер",
        "нахрен",
        "нахуй",
        "нахуйник",
        "не ебет",
        "не ебёт",
        "невротебучий",
        "невъебенно",
        "нехира",
        "нехрен",
        "Нехуй",
        "нехуйственно",
        "ниибацо",
        "ниипацца",
        "ниипаццо",
        "ниипет",
        "никуя",
        "нихера",
        "нихуя",
        "обдристаться",
        "обосранец",
        "обосрать",
        "обосцать",
        "обосцаться",
        "обсирать",
        "объебос",
        "обьебать обьебос",
        "однохуйственно",
        "опездал",
        "опизде",
        "опизденивающе",
        "остоебенить",
        "остопиздеть",
        "отмудохать",
        "отпиздить",
        "отпиздячить",
        "отпороть",
        "отъебись",
        "охуевательский",
        "охуевать",
        "охуевающий",
        "охуел",
        "охуенно",
        "охуеньчик",
        "охуеть",
        "охуительно",
        "охуительный",
        "охуяньчик",
        "охуячивать",
        "охуячить",
        "очкун",
        "падла",
        "падонки",
        "падонок",
        "паскуда",
        "педерас",
        "педик",
        "педрик",
        "педрила",
        "педрилло",
        "педрило",
        "педрилы",
        "пездень",
        "пездит",
        "пездишь",
        "пездо",
        "пездят",
        "пердануть",
        "пердеж",
        "пердение",
        "пердеть",
        "пердильник",
        "перднуть",
        "пёрднуть",
        "пердун",
        "пердунец",
        "пердунина",
        "пердунья",
        "пердуха",
        "пердь",
        "переёбок",
        "пернуть",
        "пёрнуть",
        "пи3д",
        "пи3де",
        "пи3ду",
        "пиzдец",
        "пидар",
        "пидарaс",
        "пидарас",
        "пидарасы",
        "пидары",
        "пидор",
        "пидорасы",
        "пидорка",
        "пидорок",
        "пидоры",
        "пидрас",
        "пизда",
        "пиздануть",
        "пиздануться",
        "пиздарваньчик",
        "пиздато",
        "пиздатое",
        "пиздатый",
        "пизденка",
        "пизденыш",
        "пиздёныш",
        "пиздеть",
        "пиздец",
        "пиздит",
        "пиздить",
        "пиздиться",
        "пиздишь",
        "пиздища",
        "пиздище",
        "пиздобол",
        "пиздоболы",
        "пиздобратия",
        "пиздоватая",
        "пиздоватый",
        "пиздолиз",
        "пиздонутые",
        "пиздорванец",
        "пиздорванка",
        "пиздострадатель",
        "пизду",
        "пиздуй",
        "пиздун",
        "пиздунья",
        "пизды",
        "пиздюга",
        "пиздюк",
        "пиздюлина",
        "пиздюля",
        "пиздят",
        "пиздячить",
        "писбшки",
        "писька",
        "писькострадатель",
        "писюн",
        "писюшка",
        "по хуй",
        "по хую",
        "подговнять",
        "подонки",
        "подонок",
        "подъебнуть",
        "подъебнуться",
        "поебать",
        "поебень",
        "поёбываает",
        "поскуда",
        "посрать",
        "потаскуха",
        "потаскушка",
        "похер",
        "похерил",
        "похерила",
        "похерили",
        "похеру",
        "похрен",
        "похрену",
        "похуй",
        "похуист",
        "похуистка",
        "похую",
        "придурок",
        "приебаться",
        "припиздень",
        "припизднутый",
        "припиздюлина",
        "пробзделся",
        "проблядь",
        "проеб",
        "проебанка",
        "проебать",
        "промандеть",
        "промудеть",
        "пропизделся",
        "пропиздеть",
        "пропиздячить",
        "раздолбай",
        "разхуячить",
        "разъеб",
        "разъеба",
        "разъебай",
        "разъебать",
        "распиздай",
        "распиздеться",
        "распиздяй",
        "распиздяйство",
        "распроеть",
        "сволота",
        "сволочь",
        "сговнять",
        "секель",
        "серун",
        "серька",
        "сестроеб",
        "сикель",
        "сирать",
        "сирывать",
        "соси",
        "спиздел",
        "спиздеть",
        "спиздил",
        "спиздила",
        "спиздили",
        "спиздит",
        "спиздить",
        "срака",
        "сраку",
        "сраный",
        "сранье",
        "срать",
        "срун",
        "ссака",
        "ссышь",
        "стерва",
        "страхопиздище",
        "сука",
        "суки",
        "суходрочка",
        "сучара",
        "сучий",
        "сучка",
        "сучко",
        "сучонок",
        "сучье",
        "сцание",
        "сцать",
        "сцука",
        "сцуки",
        "сцуконах",
        "сцуль",
        "сцыха",
        "сцышь",
        "съебаться",
        "сыкун",
        "трахае6",
        "трахаеб",
        "трахаёб",
        "трахатель",
        "ублюдок",
        "уебать",
        "уёбища",
        "уебище",
        "уёбище",
        "уебищное",
        "уёбищное",
        "уебк",
        "уебки",
        "уёбки",
        "уебок",
        "уёбок",
        "урюк",
        "усраться",
        "ушлепок",
        "х_у_я_р_а",
        "хyё",
        "хyй",
        "хyйня",
        "хамло",
        "хер",
        "херня",
        "херовато",
        "херовина",
        "херовый",
        "хитровыебанный",
        "хитрожопый",
        "хуeм",
        "хуе",
        "хуё",
        "хуевато",
        "хуёвенький",
        "хуевина",
        "хуево",
        "хуевый",
        "хуёвый",
        "хуек",
        "хуёк",
        "хуел",
        "хуем",
        "хуенч",
        "хуеныш",
        "хуенький",
        "хуеплет",
        "хуеплёт",
        "хуепромышленник",
        "хуерик",
        "хуерыло",
        "хуесос",
        "хуесоска",
        "хуета",
        "хуетень",
        "хуею",
        "хуи",
        "хуй",
        "хуйком",
        "хуйло",
        "хуйня",
        "хуйне",
        "хуйни",
        "хуйрик",
        "хуище",
        "хуля",
        "хую",
        "хуюл",
        "хуя",
        "хуяк",
        "хуякать",
        "хуякнуть",
        "хуяра",
        "хуясе",
        "хуячить",
        "целка",
        "чмо",
        "чмошник",
        "чмырь",
        "шалава",
        "шалавой",
        "шараёбиться",
        "шлюха",
        "шлюхой",
        "шлюшка",
        "testmsg",
        "cerf",
        ",kznm",
        ",kz",),
)