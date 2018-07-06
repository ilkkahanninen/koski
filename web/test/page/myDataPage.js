const MyDataPage = () => {
  const api = {
    openPage: () => {
      return openPage('/koski/omadata/hsl?callback=example.org', () => true )()
    },
    go: () => {
      return openPage('/koski/omadata/hsl')()
    },
    isVisible: () => {
      return isElementVisible(S('.username')) && isElementVisible(S('.user > .dateofbirth'))
    },
    login: ()  => {
      return click(findSingle('.lander button'))
    },
    delAuthCookie: () => {
      document.cookie = '_shibsession_=; path=/; expires=Thu, 01 Jan 1980 00:00:01 GMT; Max-Age=0'
    },
    addLangCookie: (lang) => {
      document.cookie = `lang=${lang}; path=/`
    },
    getUserName: () => {
      return extractAsText(S('.user > .username'))
    },
    getBirthDate: () => {
      return extractAsText(S('.user > .dateofbirth'))
    },
    getMemberName: () => {
      return extractAsText(S('.acceptance-member-name'))
    },
    clickAccept: () => {
      return click('.acceptance-button-container > .acceptance-button')()
    },
    accepted: {
      isVisible: () => {
        return isElementVisible(S('.acceptance-title-success'))
      },
      isReturnButtonVisible: () => {
        return isElementVisible(S('.acceptance-return-button'))
      }
    }
  }
  return api
}
