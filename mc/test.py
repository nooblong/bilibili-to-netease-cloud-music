import asyncio
import os
from importlib.resources import contents
import minestat
import botpy
from botpy import logging
from botpy.ext.cog_yaml import read
from botpy.message import Message, DirectMessage

test_config = read(os.path.join(os.path.dirname(__file__), "config.yaml"))

_log = logging.get_logger()


def get_stat():
    s = ""
    ms = minestat.MineStat('cn-sz-yd-plustmp2.natfrp.cloud', 21942, query_protocol=minestat.SlpProtocols.JSON)
    s += ('服务器地址: %s:%d' % (ms.address.replace(".", "点"), ms.port))
    s += "\n"
    if ms.online:
        s += ('服务器版本: %s, 在线人数: %s 总人数: %s' % (
            ms.version, ms.current_players, ms.max_players))
        s += "\n"
        if ms.player_list is not None:
            for player in ms.player_list:
                s += player
                s += "-在线 \n"
            s.rstrip("\n")
        s += ('整合包: %s' % ms.stripped_motd)
        s += "\n"

        s += ('延迟: %sms' % ms.latency)
        s += "\n"
        s += ('检查协议: %s' % ms.slp_protocol)
        s += "\n"
    else:
        s += ('服务器已关闭')
        s += "\n"
    return s


class MyClient(botpy.Client):
    async def on_ready(self):
        _log.info(f"robot 「{self.robot.name}」 on_ready!")

    async def on_group_at_message_create(self, message: Message):
        _log.info(message)
        await message.reply(content=get_stat())


if __name__ == "__main__":
    # 通过预设置的类型，设置需要监听的事件通道
    # intents = botpy.Intents.none()
    # intents.public_guild_messages=True

    # 通过kwargs，设置需要监听的事件通道
    intents = botpy.Intents(public_guild_messages=True, public_messages=True)
    client = MyClient(intents=intents)
    client.run(appid=test_config["appid"], secret=test_config["secret"])
