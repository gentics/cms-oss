/* eslint-disable @typescript-eslint/no-unsafe-call */
import moment from 'moment';
import * as romeMod from '@bevacqua/rome/dist/rome';

const romeInstance = romeMod.default;

const romeMoment = Object.assign(moment, {
    moment,
});

romeInstance.use(romeMoment);

export const rome = romeInstance;
