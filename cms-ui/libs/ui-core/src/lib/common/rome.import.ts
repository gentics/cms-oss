/* eslint-disable @typescript-eslint/no-unsafe-call */
import { getInstance } from './momentjs.import';
import * as romeMod from '@bevacqua/rome/dist/rome';

const romeInstance = romeMod.default;
romeInstance.use(getInstance());

export const rome = romeInstance;
