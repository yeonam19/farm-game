package com.farmgame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

object FontManager {
    lateinit var fontSmall: BitmapFont
    lateinit var fontMedium: BitmapFont
    lateinit var fontLarge: BitmapFont

    private var generator: FreeTypeFontGenerator? = null

    // All Korean characters actually used in the game (extracted from source code)
    private const val KOREAN_CHARS =
        "가각간갈감갑갔강개객걀거건걸검것게겨격견결겸겼경계고곡곤골공과관광괘괭교구국군굴궁권궐귀규균귤그극근글금급긍기긴길김" +
        "까깨깬꾸꾼꿈꿔꿨뀌끄끌끔끝끼" +
        "나낙낚난날낡남납낫났낭내냉너넌널넓넘넣네녀년념녕노놀농높놓놨뇌뇨누눈눌뉘뉴는늘능늦니닉닌닐님" +
        "다닥단닫달닭닮담답당닿대댁더덕던덜데도독돈돌동돼되된두둔둘드득든등디딸" +
        "땀땅때떠떡떤떨떻뗄또똑뚝뜨뜯뜻띄" +
        "라락란랄람랑래랩랭략량러럽렇레렉렌렙렛려력련렬렸령례로록론롤롭롯롱뢰료룡루룹류륙률륭르른를름릇릉릎리릭린림립링" +
        "마막만많말맑망맞맡맣매맥맨맵맺머먹먼멀멋멍멎메멜멧멩며멱면멸명몇모목몫몬몰몸몹묘무묵묶문묻물뭐뭘뭣믈미민밀밑" +
        "바박밖반발밝밟밤방밭배백뱀뱉버벅번벌범법벗벙벚베벤벨벱벼벽변별볍병볕보복볶본봄봇봉봐뵙부북분불붓붕붙붚뷰블비빈빗빙빚빛빠빡빨빵빼뺄뺨뻐뻔뻗뻣뼈뽑뿌뿐쁘쁠삐" +
        "사삭산살삶삻삼상샅새색샌샘생샤서석선설섬성세셀셋셍셔션셜셨셰소속손솔솟송쇄쇠쇼숄숍수숙순술숨숫숭숲쉬쉰쉽슈스슬슴습슷승시식신싣실싫심십싯싱싸싹싼쌀쌍쌓써썩썰썹쎄쏘쏟쑤쓰쓸씀씁씌씨씩씹씻" +
        "아악안앉않알암압앗았앙애액야약얇양얕얘어억언얼엄업없엇었엉엊엌엎에엔엘엠엡여역연열염엿영옆예옛오옥온올옮옳옹와완왔왕왜외요욕용우운울움웃웅원월웬웰위윗유육윤율으은을음읍응의이익인일읽잃임입잇있잉잊잎" +
        "자작잔잘잠잡잣잤장잦재잼잿쟁저적전절젊점정젖제져조족존졸종좋좌죄주죽준줄줌줍중줘즉즐즘즙증지직진질짊짐집짓징짖짙짚짝짧째쩔쪽쫓쭈" +
        "찍찜찝찢찧차착찬찰참창채처척천철첨첫청체쳐쳤쳬초촉촌촘총촬최추축춘출춤춥충취측층치칙친칠침" +
        "카캐캡캤켓켜켠켤켰코콘콜콤콥쾌쿠쿨퀴크큰클킨킬킵킷" +
        "타탁탄탈탐탑탓탕태택터턱털텀텁텃텅테텐템텝토톤톨톱통퇴투튀튈튜트특틀틈틉틔" +
        "파판팔패팬팁팅퍅퍼퍽페펠펴편폈평폐포폭표푹풀품풋풍프플피핀필핌핍핏핑" +
        "하학한할함합핫항해핵했행향허헌헐험헤헬헴헵헷헹혀혁현혈협혔형혜호혹혼홀홈홉홍화확환활황회획횟횡효후훈훑훔훗훤훨훼휘휩휴흉흐흑흔흙흡흥희흰히힌힐힘"

    fun init() {
        generator = FreeTypeFontGenerator(Gdx.files.internal("korean.ttf"))

        val defaultChars = FreeTypeFontGenerator.DEFAULT_CHARS +
            "0123456789!@#\$%^&*()+-=[]{}|;':\",./<>?~`" +
            KOREAN_CHARS

        // Remove duplicates
        val uniqueChars = defaultChars.toSet().joinToString("")

        fontSmall = generateFont(14, uniqueChars)
        fontMedium = generateFont(18, uniqueChars)
        fontLarge = generateFont(24, uniqueChars)
    }

    private fun generateFont(size: Int, characters: String): BitmapFont {
        val param = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            this.size = size
            this.characters = characters
            this.kerning = true
            this.genMipMaps = true
            this.minFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
            this.magFilter = com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
        }
        return generator!!.generateFont(param)
    }

    fun dispose() {
        generator?.dispose()
        fontSmall.dispose()
        fontMedium.dispose()
        fontLarge.dispose()
    }
}
