(() => {
  const bridgeType = typeof window.GasTrackAndroid?.saveFile;
  window.GasTrackAndroid.saveFile('gastrack-bridge-check.csv', 'text/csv', btoa('check,result\nandroid,ok\n'));
  return {bridgeType};
})()
