/*
 * Copyright (c) 2026 Jayvardhan Potabatti
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Vedic Mitra is free software under the GNU Affero General Public License v3.0
 * or later (see LICENSE). A commercial license is also available; see
 * LICENSING.md.
 */

package io.github.vedicmitra.feature.stotra

import java.time.DayOfWeek

/**
 * A stotra (hymn / shloka) in the bundled library.
 *
 * @property id a stable identifier.
 * @property title the common name.
 * @property deity the deity or group it belongs to, used to group the library.
 * @property devanagari the verse(s) in Devanagari.
 * @property transliteration a roman transliteration, line-for-line with [devanagari].
 * @property significance a short note on what it is and when it's recited.
 */
data class Stotra(
    val id: String,
    val title: String,
    val deity: String,
    val devanagari: String,
    val transliteration: String,
    val significance: String,
)

/**
 * The bundled catalog of stotras — all traditional (public-domain) Sanskrit. Grouped by deity, with a
 * weekday-graha suggestion for "today's stotra". Only the verse text and a short original note are
 * bundled (no full modern translation), keeping the content free of third-party translation rights.
 */
object StotraCatalog {
    /** Every stotra, clustered by deity in a sensible worship order. */
    val all: List<Stotra> =
        listOf(
            Stotra(
                id = "ganesha_vakratunda",
                title = "Vakratunda Mahakaya",
                deity = "Ganesha",
                devanagari =
                    """
                    वक्रतुण्ड महाकाय
                    सूर्यकोटि समप्रभ ।
                    निर्विघ्नं कुरु मे देव
                    सर्वकार्येषु सर्वदा ॥
                    """.trimIndent(),
                transliteration =
                    """
                    vakratunda mahakaya
                    surya-koti samaprabha
                    nirvighnam kuru me deva
                    sarva-karyeshu sarvada
                    """.trimIndent(),
                significance = "A prayer to remove obstacles, invoked before beginning any task or worship.",
            ),
            Stotra(
                id = "ganesha_shuklambara",
                title = "Shuklambaradharam",
                deity = "Ganesha",
                devanagari =
                    """
                    शुक्लाम्बरधरं विष्णुं
                    शशिवर्णं चतुर्भुजम् ।
                    प्रसन्नवदनं ध्यायेत्
                    सर्वविघ्नोपशान्तये ॥
                    """.trimIndent(),
                transliteration =
                    """
                    shuklambara-dharam vishnum
                    shashi-varnam chatur-bhujam
                    prasanna-vadanam dhyayet
                    sarva-vighnopashantaye
                    """.trimIndent(),
                significance = "A meditation on Ganesha for the calming of all obstacles, recited to open prayers.",
            ),
            Stotra(
                id = "mahamrityunjaya",
                title = "Mahamrityunjaya Mantra",
                deity = "Shiva",
                devanagari =
                    """
                    ॐ त्र्यम्बकं यजामहे
                    सुगन्धिं पुष्टिवर्धनम् ।
                    उर्वारुकमिव बन्धनान्
                    मृत्योर्मुक्षीय माऽमृतात् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om tryambakam yajamahe
                    sugandhim pushti-vardhanam
                    urvarukamiva bandhanan
                    mrityor mukshiya maamritat
                    """.trimIndent(),
                significance =
                    "The great victory-over-death mantra to three-eyed Shiva, prayed for healing and longevity.",
            ),
            Stotra(
                id = "shiva_karpura",
                title = "Karpura Gauram",
                deity = "Shiva",
                devanagari =
                    """
                    कर्पूरगौरं करुणावतारं
                    संसारसारं भुजगेन्द्रहारम् ।
                    सदा वसन्तं हृदयारविन्दे
                    भवं भवानीसहितं नमामि ॥
                    """.trimIndent(),
                transliteration =
                    """
                    karpura-gauram karunavataram
                    samsara-saram bhujagendra-haram
                    sada vasantam hridayaravinde
                    bhavam bhavani-sahitam namami
                    """.trimIndent(),
                significance = "A camphor-white image of Shiva with Parvati, dwelling ever in the lotus of the heart.",
            ),
            Stotra(
                id = "shiva_panchakshara",
                title = "Nagendraharaya",
                deity = "Shiva",
                devanagari =
                    """
                    नागेन्द्रहाराय त्रिलोचनाय
                    भस्माङ्गरागाय महेश्वराय ।
                    नित्याय शुद्धाय दिगम्बराय
                    तस्मै न काराय नमः शिवाय ॥
                    """.trimIndent(),
                transliteration =
                    """
                    nagendra-haraya tri-lochanaya
                    bhasmanga-ragaya maheshvaraya
                    nityaya shuddhaya digambaraya
                    tasmai na-karaya namah shivaya
                    """.trimIndent(),
                significance = "The opening verse of the Shiva Panchakshara Stotra, glorifying \"Namah Shivaya\".",
            ),
            Stotra(
                id = "vishnu_shantakaram",
                title = "Shantakaram",
                deity = "Vishnu",
                devanagari =
                    """
                    शान्ताकारं भुजगशयनं
                    पद्मनाभं सुरेशम् ।
                    विश्वाधारं गगनसदृशं
                    मेघवर्णं शुभाङ्गम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    shantakaram bhujaga-shayanam
                    padmanabham suresham
                    vishvadharam gagana-sadrisham
                    megha-varnam shubhangam
                    """.trimIndent(),
                significance = "A serene meditation on Vishnu reclining on the cosmic serpent Shesha.",
            ),
            Stotra(
                id = "krishna_vasudeva",
                title = "Vasudevasutam",
                deity = "Krishna",
                devanagari =
                    """
                    वसुदेवसुतं देवं
                    कंसचाणूरमर्दनम् ।
                    देवकीपरमानन्दं
                    कृष्णं वन्दे जगद्गुरुम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    vasudeva-sutam devam
                    kamsa-chanura-mardanam
                    devaki-paramanandam
                    krishnam vande jagad-gurum
                    """.trimIndent(),
                significance = "A vandana to Krishna, teacher of the world and joy of Devaki.",
            ),
            Stotra(
                id = "rama_nama",
                title = "Shri Rama Rama Rameti",
                deity = "Rama",
                devanagari =
                    """
                    श्रीरामं रामरामेति
                    रमे रामे मनोरमे ।
                    सहस्रनाम तत्तुल्यं
                    रामनाम वरानने ॥
                    """.trimIndent(),
                transliteration =
                    """
                    shri-ramam rama-rameti
                    rame rame manorame
                    sahasra-nama tat-tulyam
                    rama-nama varanane
                    """.trimIndent(),
                significance = "The verse in which repeating \"Rama\" is said to equal a thousand names of the Lord.",
            ),
            Stotra(
                id = "devi_ya_devi",
                title = "Ya Devi Sarvabhuteshu",
                deity = "Devi",
                devanagari =
                    """
                    या देवी सर्वभूतेषु
                    शक्तिरूपेण संस्थिता ।
                    नमस्तस्यै नमस्तस्यै
                    नमस्तस्यै नमो नमः ॥
                    """.trimIndent(),
                transliteration =
                    """
                    ya devi sarva-bhuteshu
                    shakti-rupena samsthita
                    namas-tasyai namas-tasyai
                    namas-tasyai namo namah
                    """.trimIndent(),
                significance = "From the Devi Mahatmyam, saluting the Goddess who dwells in all beings as power.",
            ),
            Stotra(
                id = "durga_sarva_mangala",
                title = "Sarva Mangala Mangalye",
                deity = "Devi",
                devanagari =
                    """
                    सर्वमङ्गलमाङ्गल्ये
                    शिवे सर्वार्थसाधिके ।
                    शरण्ये त्र्यम्बके गौरि
                    नारायणि नमोऽस्तु ते ॥
                    """.trimIndent(),
                transliteration =
                    """
                    sarva-mangala-mangalye
                    shive sarvartha-sadhike
                    sharanye tryambake gauri
                    narayani namo'stu te
                    """.trimIndent(),
                significance = "A beloved prayer to the auspicious Goddess who fulfils all aims and grants refuge.",
            ),
            Stotra(
                id = "lakshmi",
                title = "Namaste'stu Mahamaye",
                deity = "Lakshmi",
                devanagari =
                    """
                    नमस्तेऽस्तु महामाये
                    श्रीपीठे सुरपूजिते ।
                    शङ्खचक्रगदाहस्ते
                    महालक्ष्मि नमोऽस्तु ते ॥
                    """.trimIndent(),
                transliteration =
                    """
                    namaste'stu maha-maye
                    shri-pithe sura-pujite
                    shankha-chakra-gada-haste
                    maha-lakshmi namo'stu te
                    """.trimIndent(),
                significance = "A salutation to Mahalakshmi, goddess of abundance; recited on Fridays and Diwali.",
            ),
            Stotra(
                id = "saraswati",
                title = "Ya Kundendu Tushara",
                deity = "Saraswati",
                devanagari =
                    """
                    या कुन्देन्दुतुषारहारधवला
                    या शुभ्रवस्त्रावृता ।
                    या वीणावरदण्डमण्डितकरा
                    या श्वेतपद्मासना ॥
                    """.trimIndent(),
                transliteration =
                    """
                    ya kundendu-tushara-hara-dhavala
                    ya shubhra-vastravrita
                    ya vina-vara-danda-mandita-kara
                    ya shveta-padmasana
                    """.trimIndent(),
                significance = "A vandana to Saraswati, goddess of knowledge, music and speech; recited by students.",
            ),
            Stotra(
                id = "gayatri",
                title = "Gayatri Mantra",
                deity = "Surya",
                devanagari =
                    """
                    ॐ भूर्भुवः स्वः ।
                    तत्सवितुर्वरेण्यं
                    भर्गो देवस्य धीमहि ।
                    धियो यो नः प्रचोदयात् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om bhur bhuvah svah
                    tat savitur varenyam
                    bhargo devasya dhimahi
                    dhiyo yo nah prachodayat
                    """.trimIndent(),
                significance = "The supreme Vedic prayer to Savitr (the Sun) to awaken and guide the mind.",
            ),
            Stotra(
                id = "guru",
                title = "Gurur Brahma",
                deity = "Guru",
                devanagari =
                    """
                    गुरुर्ब्रह्मा गुरुर्विष्णुः
                    गुरुर्देवो महेश्वरः ।
                    गुरुः साक्षात् परब्रह्म
                    तस्मै श्रीगुरवे नमः ॥
                    """.trimIndent(),
                transliteration =
                    """
                    gurur brahma gurur vishnuh
                    gurur devo maheshvarah
                    guruh sakshat para-brahma
                    tasmai shri-gurave namah
                    """.trimIndent(),
                significance = "Honours the guru as Brahma, Vishnu and Shiva; recited on Guru Purnima and Thursdays.",
            ),
            Stotra(
                id = "hanuman",
                title = "Manojavam Marutatulya",
                deity = "Hanuman",
                devanagari =
                    """
                    मनोजवं मारुततुल्यवेगं
                    जितेन्द्रियं बुद्धिमतां वरिष्ठम् ।
                    वातात्मजं वानरयूथमुख्यं
                    श्रीरामदूतं शरणं प्रपद्ये ॥
                    """.trimIndent(),
                transliteration =
                    """
                    manojavam maruta-tulya-vegam
                    jitendriyam buddhimatam varishtham
                    vatatmajam vanara-yutha-mukhyam
                    shri-rama-dutam sharanam prapadye
                    """.trimIndent(),
                significance = "A dhyana on Hanuman — swift, wise, and devoted; recited on Tuesdays and Saturdays.",
            ),
            Stotra(
                id = "navagraha",
                title = "Navagraha Stotra",
                deity = "Navagraha",
                devanagari =
                    """
                    जपाकुसुमसंकाशं काश्यपेयं महाद्युतिम् ।
                    तमोऽरिं सर्वपापघ्नं प्रणतोऽस्मि दिवाकरम् ॥
                    दधिशङ्खतुषाराभं क्षीरोदार्णवसम्भवम् ।
                    नमामि शशिनं सोमं शम्भोर्मुकुटभूषणम् ॥
                    धरणीगर्भसम्भूतं विद्युत्कान्तिसमप्रभम् ।
                    कुमारं शक्तिहस्तं तं मङ्गलं प्रणमाम्यहम् ॥
                    प्रियङ्गुकलिकाश्यामं रूपेणाप्रतिमं बुधम् ।
                    सौम्यं सौम्यगुणोपेतं तं बुधं प्रणमाम्यहम् ॥
                    देवानां च ऋषीणां च गुरुं काञ्चनसन्निभम् ।
                    बुद्धिभूतं त्रिलोकेशं तं नमामि बृहस्पतिम् ॥
                    हिमकुन्दमृणालाभं दैत्यानां परमं गुरुम् ।
                    सर्वशास्त्रप्रवक्तारं भार्गवं प्रणमाम्यहम् ॥
                    नीलाञ्जनसमाभासं रविपुत्रं यमाग्रजम् ।
                    छायामार्तण्डसम्भूतं तं नमामि शनैश्चरम् ॥
                    अर्धकायं महावीर्यं चन्द्रादित्यविमर्दनम् ।
                    सिंहिकागर्भसम्भूतं तं राहुं प्रणमाम्यहम् ॥
                    पलाशपुष्पसंकाशं तारकाग्रहमस्तकम् ।
                    रौद्रं रौद्रात्मकं घोरं तं केतुं प्रणमाम्यहम् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    japa-kusuma-sankasham kashyapeyam maha-dyutim
                    tamo'rim sarva-papaghnam pranato'smi divakaram
                    dadhi-shankha-tusharabham kshirodarnava-sambhavam
                    namami shashinam somam shambhor-mukuta-bhushanam
                    dharani-garbha-sambhutam vidyut-kanti-samaprabham
                    kumaram shakti-hastam tam mangalam pranamamy-aham
                    priyangu-kalika-shyamam rupena-apratimam budham
                    saumyam saumya-gunopetam tam budham pranamamy-aham
                    devanam cha rishinam cha gurum kanchana-sannibham
                    buddhi-bhutam tri-lokesham tam namami brihaspatim
                    hima-kunda-mrinalabham daityanam paramam gurum
                    sarva-shastra-pravaktaram bhargavam pranamamy-aham
                    nilanjana-samabhasam ravi-putram yamagrajam
                    chhaya-martanda-sambhutam tam namami shanaischaram
                    ardha-kayam maha-viryam chandraditya-vimardanam
                    simhika-garbha-sambhutam tam rahum pranamamy-aham
                    palasha-pushpa-sankasham tarakagraha-mastakam
                    raudram raudratmakam ghoram tam ketum pranamamy-aham
                    """.trimIndent(),
                significance = "The nine short verses to the nine grahas (Sun through Ketu), one couplet for each.",
            ),
            Stotra(
                id = "shanti_sarve",
                title = "Sarve Bhavantu Sukhinah",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ॐ सर्वे भवन्तु सुखिनः
                    सर्वे सन्तु निरामयाः ।
                    सर्वे भद्राणि पश्यन्तु
                    मा कश्चिद्दुःखभाग्भवेत् ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om sarve bhavantu sukhinah
                    sarve santu niramayah
                    sarve bhadrani pashyantu
                    ma kashchid duhkha-bhag bhavet
                    """.trimIndent(),
                significance = "A universal prayer that all beings be happy, healthy, and free from suffering.",
            ),
            Stotra(
                id = "shanti_asato",
                title = "Asato Ma Sadgamaya",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ॐ असतो मा सद्गमय ।
                    तमसो मा ज्योतिर्गमय ।
                    मृत्योर्मा अमृतं गमय ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om asato ma sad-gamaya
                    tamaso ma jyotir-gamaya
                    mrityor ma amritam gamaya
                    """.trimIndent(),
                significance =
                    "From the Brihadaranyaka Upanishad — lead me from the unreal to the real, dark to light.",
            ),
            Stotra(
                id = "shanti_purnam",
                title = "Purnamadah Purnamidam",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ॐ पूर्णमदः पूर्णमिदं
                    पूर्णात्पूर्णमुदच्यते ।
                    पूर्णस्य पूर्णमादाय
                    पूर्णमेवावशिष्यते ॥
                    """.trimIndent(),
                transliteration =
                    """
                    om purnam-adah purnam-idam
                    purnat purnam-udachyate
                    purnasya purnam-adaya
                    purnam-eva-avashishyate
                    """.trimIndent(),
                significance = "The Isha Upanishad invocation on the fullness of the Absolute.",
            ),
            Stotra(
                id = "twameva",
                title = "Tvameva Mata",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    त्वमेव माता च पिता त्वमेव
                    त्वमेव बन्धुश्च सखा त्वमेव ।
                    त्वमेव विद्या द्रविणं त्वमेव
                    त्वमेव सर्वं मम देवदेव ॥
                    """.trimIndent(),
                transliteration =
                    """
                    tvameva mata cha pita tvameva
                    tvameva bandhush cha sakha tvameva
                    tvameva vidya dravinam tvameva
                    tvameva sarvam mama deva-deva
                    """.trimIndent(),
                significance = "A surrender prayer — you alone are mother, father, friend, knowledge, my all.",
            ),
            Stotra(
                id = "brahmarpanam",
                title = "Brahmarpanam",
                deity = "Shanti & Universal",
                devanagari =
                    """
                    ब्रह्मार्पणं ब्रह्म हविः
                    ब्रह्माग्नौ ब्रह्मणा हुतम् ।
                    ब्रह्मैव तेन गन्तव्यं
                    ब्रह्मकर्मसमाधिना ॥
                    """.trimIndent(),
                transliteration =
                    """
                    brahmarpanam brahma havih
                    brahmagnau brahmana hutam
                    brahmaiva tena gantavyam
                    brahma-karma-samadhina
                    """.trimIndent(),
                significance = "A Bhagavad Gita verse (4.24) recited before meals — all is Brahman.",
            ),
        )

    private val byId: Map<String, Stotra> = all.associateBy { it.id }

    /** The library grouped by deity, preserving the catalogue's worship order. */
    val byDeity: Map<String, List<Stotra>> = all.groupBy { it.deity }

    /** The stotra with [id], or `null` if it isn't in the catalog. */
    fun byId(id: String): Stotra? = byId[id]

    /**
     * The "today's stotra" for [day], chosen by the weekday's ruling graha/deity: Sunday–Surya,
     * Monday–Shiva/Chandra, Tuesday–Hanuman/Mangala, Wednesday–Vishnu/Budha, Thursday–Guru,
     * Friday–Lakshmi/Shukra, Saturday–the Navagraha (Shani).
     */
    fun forWeekday(day: DayOfWeek): Stotra {
        val id =
            when (day) {
                DayOfWeek.SUNDAY -> "gayatri"
                DayOfWeek.MONDAY -> "shiva_karpura"
                DayOfWeek.TUESDAY -> "hanuman"
                DayOfWeek.WEDNESDAY -> "vishnu_shantakaram"
                DayOfWeek.THURSDAY -> "guru"
                DayOfWeek.FRIDAY -> "lakshmi"
                DayOfWeek.SATURDAY -> "navagraha"
            }
        return requireNotNull(byId[id]) { "weekday stotra $id missing from catalog" }
    }
}
